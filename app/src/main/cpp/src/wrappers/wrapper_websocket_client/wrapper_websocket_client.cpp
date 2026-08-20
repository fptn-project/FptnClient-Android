/*
 * FPTN Android Client
 * Copyright (C) 2026  Skokov Stanislav, Enin Sergey
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Website: https://fptn.org
 */


#include <chrono>
#include <jni.h>
#include <utility>

#include "jnienv/jnienv.h"
#include "spdlog/spdlog.h"
#include "wrapper_websocket_client.h"

#include "fptn-protocol-lib/connection/strategies/browser_mimicry/browser_mimicry.h"
#include "fptn-protocol-lib/connection/strategies/rolling_tunnel/rolling_tunnel.h"

#ifndef FPTN_CLIENT_DEFAULT_ADDRESS_IP6
#define FPTN_CLIENT_DEFAULT_ADDRESS_IP6 "fd00::1"
#endif

namespace fptn::wrapper {

WrapperWebsocketClient::WrapperWebsocketClient(jobject wrapper,
    std::string server_ip,
    int server_port,
    std::string tun_ipv4,
    std::string tun_ipv6,
    std::string sni,
    std::string access_token,
    std::string expected_md5_fingerprint,
    std::string client_version,
    fptn::protocol::https::CensorshipStrategy censorship_strategy,
    fptn::protocol::connection::strategies::ConnectionStrategy
        connection_strategy)
    : running_(false),
      reconnection_attempts_(kMaxReconnectionAttempts_),
      wrapper_(wrapper),
      server_ip_(std::move(server_ip)),
      server_port_(server_port),
      sni_(std::move(sni)),
      tun_ipv4_(tun_ipv4),
      tun_ipv6_(tun_ipv6),
      access_token_(std::move(access_token)),
      expected_md5_fingerprint_(std::move(expected_md5_fingerprint)),
      client_version_(std::move(client_version)),
      censorship_strategy_(censorship_strategy),
      connection_strategy_(connection_strategy)
      {}

WrapperWebsocketClient::~WrapperWebsocketClient() {
  Stop();
  if (byte_array_class_) {
    if (JNIEnv* env = getJniEnv()) {
      env->DeleteGlobalRef(byte_array_class_);
    }
    byte_array_class_ = nullptr;
  }
}

bool WrapperWebsocketClient::Start() {
  const std::unique_lock<std::mutex> lock(mutex_);

  if (running_) {
    return false;  // Already running
  }

  running_ = true;
  th_ = std::thread(&WrapperWebsocketClient::Run, this);
  return th_.joinable();
}

bool WrapperWebsocketClient::Stop() {
  auto safe_join = [this]() {
    if (!th_.joinable()) {
        return;
    }
    if (std::this_thread::get_id() == th_.get_id()) {
      th_.detach();
    } else {
      th_.join();
    }
  };

  if (!running_) {
    safe_join();
    return true;
  }
  {
    const std::unique_lock<std::mutex> lock(mutex_);
    if (!running_) {
      return false;
    }
    running_ = false;

    if (client_) {
      client_->Stop();
    }
  }

  safe_join();

  {
    const std::unique_lock<std::mutex> lock(mutex_);
    client_.reset();
  }

  return true;
}

bool WrapperWebsocketClient::IsStarted() {
  return client_ && running_ && client_->IsStarted();
}

void WrapperWebsocketClient::Run() {
  // Time window for counting attempts (1 minute)
  constexpr auto kReconnectionWindow = std::chrono::seconds(60);
  // Delay between reconnection attempts
  constexpr auto kReconnectionDelay = std::chrono::milliseconds(200);

  // Current count of reconnection attempts
  reconnection_attempts_ = kMaxReconnectionAttempts_;
  window_start_time_ = std::chrono::steady_clock::now();

  while (running_ && reconnection_attempts_ > 0) {
    try {
      const auto server_ip_addr = fptn::common::network::IPv4Address::Create(server_ip_);

      if (!server_ip_addr.IsValid()) {
        SPDLOG_ERROR("Invalid IP address configuration - server: {}", server_ip_);
        break;
      }

      {
        const std::unique_lock<std::mutex> lock(mutex_);  // mutex

        const auto new_ip_pkt_callback = std::bind(
            &WrapperWebsocketClient::onIPPackets, this,
            std::placeholders::_1);
        const auto on_connected_callback =
            std::bind(&WrapperWebsocketClient::onConnectedCallback, this);
        const auto on_socket_opened_callback = std::bind(
            &WrapperWebsocketClient::onSocketOpened, this, std::placeholders::_1);
        const fptn::protocol::https::ConnectionConfig config{
            .common = {
                .server_ip = server_ip_addr,
                .server_port = static_cast<std::uint16_t>(server_port_),
                .sni = sni_,
                .md5_fingerprint = expected_md5_fingerprint_,
                .client_version = client_version_,
                .censorship_strategy = censorship_strategy_,
                .tun_interface_address_ipv4 =
                    fptn::common::network::IPv4Address(tun_ipv4_),
                .tun_interface_address_ipv6 =
                    fptn::common::network::IPv6Address(tun_ipv6_),
                .on_connected_callback = on_connected_callback,
                .recv_ip_packet_batch_callback = new_ip_pkt_callback,
                .on_socket_opened_callback = on_socket_opened_callback,
            }};

        namespace strategies = fptn::protocol::connection::strategies;
        switch (connection_strategy_) {
          case strategies::ConnectionStrategy::kDualRollingTunnel:
            client_ =
                strategies::DualRollingTunnel::Create(access_token_, config);
            break;
          case strategies::ConnectionStrategy::kTripleRollingTunnel:
            client_ =
                strategies::TripleRollingTunnel::Create(access_token_, config);
            break;
          case strategies::ConnectionStrategy::kBrowserMimicry:
            client_ = strategies::BrowserMimicry::Create(access_token_, config);
            break;
          case strategies::ConnectionStrategy::kSingleRollingTunnel:
          default:
            client_ =
                strategies::SingleRollingTunnel::Create(access_token_, config);
            break;
        }
      }
      if (running_ && client_) {
        client_->Start();
      }
    } catch (const std::exception& ex) {
      SPDLOG_ERROR("Exception during client run: {}", ex.what());
    } catch (...) {
      SPDLOG_ERROR("Unknown exception during client run");
    }

    if (!running_) {
      break;
    }

    // Calculate time since last window start
    const auto current_time = std::chrono::steady_clock::now();
    const auto elapsed = current_time - window_start_time_;

    // Reconnection attempt counting logic
    if (elapsed >= kReconnectionWindow) {
      // Reset counter if we're past the time window
      reconnection_attempts_ = kMaxReconnectionAttempts_;
      window_start_time_ = current_time;
    } else {
      // Decrement counter if within time window
      --reconnection_attempts_;
    }

    if (running_ && reconnection_attempts_ > 0) {
      // Log connection failure and wait before retrying
      SPDLOG_ERROR(
          "Connection closed (attempt {}/{} in current window). Reconnecting "
          "in {}ms...",
          kMaxReconnectionAttempts_ - reconnection_attempts_,
          kMaxReconnectionAttempts_, kReconnectionDelay.count());
      std::this_thread::sleep_for(kReconnectionDelay);
    }
  }

  // Final failure handler
  if (running_ && reconnection_attempts_ == 0) {
    SPDLOG_ERROR("Failed to establish connection after {} attempts",
        kMaxReconnectionAttempts_);
    running_ = false;

    JNIEnv* env = getJniEnv();
    if (!env) {
      SPDLOG_ERROR("JNIEnv is null in final failure block");
      return;
    }

    jclass cls = env->GetObjectClass(wrapper_);
    if (!cls) {
      SPDLOG_ERROR("Failed to get Java class from wrapper_");
      return;
    }

    jmethodID on_failure_impl = env->GetMethodID(cls, "onFailureImpl", "()V");
    if (on_failure_impl) {
      env->CallVoidMethod(wrapper_, on_failure_impl);
      if (env->ExceptionCheck()) {
        SPDLOG_ERROR("JNI Exception in CallVoidMethod(onFailureImpl)");
        env->ExceptionDescribe();
        env->ExceptionClear();
      }
    } else {
      SPDLOG_ERROR("Failed to find method ID for onFailureImpl()");
    }

    env->DeleteLocalRef(cls);
  }
}

bool WrapperWebsocketClient::ResolveMessageCallback(JNIEnv* env) {
  if (on_message_impl_ && byte_array_class_) {
    return true;
  }

  jclass cls = env->GetObjectClass(wrapper_);
  if (!cls) {
    SPDLOG_ERROR("Failed to get object class");
    return false;
  }
  on_message_impl_ = env->GetMethodID(cls, "onMessageImpl", "([[B)V");
  env->DeleteLocalRef(cls);
  if (!on_message_impl_) {
    SPDLOG_ERROR("Failed to get method ID: onMessageImpl([[B)V");
    return false;
  }

  jclass byte_array_cls = env->FindClass("[B");
  if (!byte_array_cls) {
    SPDLOG_ERROR("Failed to find byte[] class");
    on_message_impl_ = nullptr;
    return false;
  }
  // Global: the method id stays valid only while its class is alive.
  byte_array_class_ = static_cast<jclass>(env->NewGlobalRef(byte_array_cls));
  env->DeleteLocalRef(byte_array_cls);
  return byte_array_class_ != nullptr;
}

void WrapperWebsocketClient::onIPPackets(
    fptn::common::network::BatchIPPacketPtr packets) {
  if (packets.empty() || !running_) {
    return;
  }

  JNIEnv* env = getJniEnv();
  if (!env) {
    SPDLOG_ERROR("Failed to get JNI environment in onIPPackets");
    return;
  }
  if (!ResolveMessageCallback(env)) {
    return;
  }

  jsize count = 0;
  for (const auto& packet : packets) {
    if (packet && !packet->Data().empty()) {
      ++count;
    }
  }
  if (count == 0) {
    return;
  }

  jobjectArray jpackets = nullptr;
  try {
    jpackets = env->NewObjectArray(count, byte_array_class_, nullptr);
    if (!jpackets) {
      SPDLOG_ERROR("Failed to allocate jobjectArray");
      return;
    }

    jsize index = 0;
    for (const auto& packet : packets) {
      if (!packet || packet->Data().empty()) {
        continue;
      }
      const auto& data = packet->Data();
      const auto len = static_cast<jsize>(data.size());

      jbyteArray jpacket = env->NewByteArray(len);
      if (!jpacket) {
        SPDLOG_ERROR("Failed to allocate jbyteArray");
        break;
      }
      env->SetByteArrayRegion(
          jpacket, 0, len, reinterpret_cast<const jbyte*>(data.data()));
      env->SetObjectArrayElement(jpackets, index++, jpacket);
      // Released right away so a large batch cannot exhaust the local table.
      env->DeleteLocalRef(jpacket);
    }

    if (env->ExceptionCheck()) {
      SPDLOG_ERROR("JNI Exception while building the packet array");
      env->ExceptionDescribe();
      env->ExceptionClear();
    } else {
      env->CallVoidMethod(wrapper_, on_message_impl_, jpackets);
      if (env->ExceptionCheck()) {
        SPDLOG_ERROR("JNI Exception in CallVoidMethod");
        env->ExceptionDescribe();
        env->ExceptionClear();
      }
    }
  } catch (const std::exception& ex) {
    SPDLOG_ERROR("Exception in onIPPackets: {}", ex.what());
  } catch (...) {
    SPDLOG_ERROR("Unknown exception in onIPPackets");
  }

  if (jpackets) {
    env->DeleteLocalRef(jpackets);
  }
}

void WrapperWebsocketClient::onConnectedCallback() {
  if (!running_.load()) {
    SPDLOG_WARN("onConnectedCallback called but client is not running");
    return;
  }

  reconnection_attempts_ = kMaxReconnectionAttempts_;
  window_start_time_ = std::chrono::steady_clock::now();

  // onOpenImpl fires only once per object lifetime
  bool expected = false;
  if (!has_opened_.compare_exchange_strong(expected, true)) {
    SPDLOG_INFO("onConnectedCallback: already notified Java once — skipping onOpenImpl");
    return;
  }

  JNIEnv* env = getJniEnv();
  if (!env) {
    SPDLOG_ERROR("Failed to get JNI environment in onConnectedCallback");
    return;
  }

  jclass cls = env->GetObjectClass(wrapper_);
  if (!cls) {
    SPDLOG_ERROR("Failed to get Java class from wrapper_");
    return;
  }

  jmethodID on_open_impl = env->GetMethodID(cls, "onOpenImpl", "()V");
  if (on_open_impl) {
    env->CallVoidMethod(wrapper_, on_open_impl);
    if (env->ExceptionCheck()) {
      SPDLOG_ERROR("JNI Exception in CallVoidMethod for onOpenImpl()");
      env->ExceptionDescribe();
      env->ExceptionClear();
    }
  } else {
    SPDLOG_ERROR("Failed to find method ID for onOpenImpl()");
  }
  env->DeleteLocalRef(cls);
}

void WrapperWebsocketClient::onSocketOpened(int socket_fd) {
  JNIEnv* env = getJniEnv();
  if (!env) {
    SPDLOG_ERROR("Failed to get JNI environment in onSocketOpened");
    return;
  }

  jclass cls = env->GetObjectClass(wrapper_);
  if (!cls) {
    SPDLOG_ERROR("Failed to get Java class from wrapper_ in onSocketOpened");
    return;
  }

  jmethodID on_socket_opened_impl =
      env->GetMethodID(cls, "onSocketOpenedImpl", "(I)V");
  if (on_socket_opened_impl) {
    env->CallVoidMethod(
        wrapper_, on_socket_opened_impl, static_cast<jint>(socket_fd));
    if (env->ExceptionCheck()) {
      SPDLOG_ERROR("JNI Exception in CallVoidMethod for onSocketOpenedImpl(I)V");
      env->ExceptionDescribe();
      env->ExceptionClear();
    }
  } else {
    SPDLOG_ERROR("Failed to find method ID for onSocketOpenedImpl(I)V");
  }
  env->DeleteLocalRef(cls);
}

void WrapperWebsocketClient::onIpAssignedCallback(const fptn::common::network::IPv4Address& ipv4,
                              const fptn::common::network::IPv6Address& ipv6) {
  SPDLOG_INFO("onIpAssignedCallback: {}, {}", ipv4.ToString(), ipv6.ToString());
  ipv4_address_ = ipv4;
  ipv6_address_ = ipv6;
}

const fptn::common::network::IPv4Address& WrapperWebsocketClient::IPv4Address() const {
  return ipv4_address_;
}

const fptn::common::network::IPv6Address& WrapperWebsocketClient::IPv6Address() const {
  return ipv6_address_;
}

bool WrapperWebsocketClient::Send(fptn::common::network::IPPacketData data) {
  if (!running_) {
    return false;
  }
  try {
    auto ip_packet = fptn::common::network::IPPacket::Parse(std::move(data));
    if (!ip_packet) {
      SPDLOG_ERROR("Failed to parse IP packet in Send");
      return false;
    }
    if (running_ && client_ && client_->IsStarted()) {
      const std::unique_lock<std::mutex> lock(mutex_);  // mutex

      // cppcheck-suppress identicalConditionAfterEarlyExit
      if (!running_ || !client_ || !client_->IsStarted()) {
        return false;
      }
      client_->Send(std::move(ip_packet));
      return true;
    }
  } catch (const std::exception& ex) {
    SPDLOG_ERROR("Exception in Send: {}", ex.what());
  } catch (...) {
    SPDLOG_ERROR("Unknown exception in Send");
  }
  return false;
}

}  // namespace fptn::wrapper
