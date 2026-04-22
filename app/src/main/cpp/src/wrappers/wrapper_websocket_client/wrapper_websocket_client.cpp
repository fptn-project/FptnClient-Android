/*=============================================================================
Copyright (c) 2024-2025 Stas Skokov
Copyright (c) 2024-2025 brightsunshine54

Distributed under the MIT License (https://opensource.org/licenses/MIT)
=============================================================================*/

#include <chrono>
#include <jni.h>
#include <utility>

#include "jnienv/jnienv.h"
#include "spdlog/spdlog.h"
#include "wrapper_websocket_client.h"

#ifndef FPTN_CLIENT_DEFAULT_ADDRESS_IP6
#define FPTN_CLIENT_DEFAULT_ADDRESS_IP6 "fd00::1"
#endif

namespace fptn::wrapper {

WrapperWebsocketClient::WrapperWebsocketClient(jobject wrapper,
    std::string server_ip,
    int server_port,
    std::string tun_ipv4,
    std::string sni,
    std::string access_token,
    std::string expected_md5_fingerprint,
    fptn::protocol::https::CensorshipStrategy censorship_strategy)
    : running_(false),
      reconnection_attempts_(kMaxReconnectionAttempts_),
      wrapper_(wrapper),
      server_ip_(std::move(server_ip)),
      server_port_(server_port),
      tun_ipv4_(std::move(tun_ipv4)),
      sni_(std::move(sni)),
      access_token_(std::move(access_token)),
      expected_md5_fingerprint_(std::move(expected_md5_fingerprint)),
      censorship_strategy_(censorship_strategy)
      {}

WrapperWebsocketClient::~WrapperWebsocketClient() { Stop(); }

bool WrapperWebsocketClient::Start() {
  std::unique_lock<std::mutex> lock(mutex_);

  if (running_) {
    return false;  // Already running
  }

  running_ = true;
  th_ = std::thread(&WrapperWebsocketClient::Run, this);
  return th_.joinable();
}

bool WrapperWebsocketClient::Stop() {
  if (!running_) {
    return true;
  }
  {
    const std::unique_lock<std::mutex> lock(mutex_);  // mutex
    if (!running_) {
      return false;
    }
    running_ = false;

    if (client_) {
      client_->Stop();

      std::this_thread::sleep_for(std::chrono::milliseconds(100));

      client_.reset();
    }
  }

  if (th_.joinable()) {
    th_.join();
  }
  return true;
}

bool WrapperWebsocketClient::IsStarted() {
  return client_ && running_;
}

void WrapperWebsocketClient::Run() {
  // Time window for counting attempts (1 minute)
  constexpr auto kReconnectionWindow = std::chrono::seconds(60);
  // Delay between reconnection attempts
  constexpr auto kReconnectionDelay = std::chrono::milliseconds(300);

  // Current count of reconnection attempts
  reconnection_attempts_ = kMaxReconnectionAttempts_;
  auto window_start_time = std::chrono::steady_clock::now();

  while (running_ && reconnection_attempts_ > 0) {
    try {
      const auto server_ip_addr = fptn::common::network::IPv4Address::Create(server_ip_);
      const auto tun_ipv4_addr = fptn::common::network::IPv4Address::Create(tun_ipv4_);
      const auto tun_ipv6_addr = fptn::common::network::IPv6Address::Create(
              FPTN_CLIENT_DEFAULT_ADDRESS_IP6);

      if (!server_ip_addr.IsValid() || !tun_ipv4_addr.IsValid() || !tun_ipv6_addr.IsValid()) {
        SPDLOG_ERROR(
            "Invalid IP address configuration - server: {}, tun_ipv4: {}, tun_ipv6: {}",
            server_ip_, tun_ipv4_, FPTN_CLIENT_DEFAULT_ADDRESS_IP6);
        break;
      }

      {
        const std::unique_lock<std::mutex> lock(mutex_);  // mutex

        auto new_ip_pkt_callback = std::bind(
            &WrapperWebsocketClient::onIPPacket, this, std::placeholders::_1);
        auto on_connected_callback =
            std::bind(&WrapperWebsocketClient::onConnectedCallback, this);

          client_ = std::make_shared<fptn::protocol::https::WebsocketClient>(
            server_ip_addr,
            server_port_,
            tun_ipv4_addr,
            tun_ipv6_addr,
            new_ip_pkt_callback,
            sni_,
            access_token_,
            expected_md5_fingerprint_,
            censorship_strategy_,
            on_connected_callback
        );
      }

      if (running_ && client_) {
        client_->Run();
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
    const auto elapsed = current_time - window_start_time;

    // Reconnection attempt counting logic
    if (elapsed >= kReconnectionWindow) {
      // Reset counter if we're past the time window
      reconnection_attempts_ = kMaxReconnectionAttempts_;
      window_start_time = current_time;
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

void WrapperWebsocketClient::onIPPacket(
    fptn::common::network::IPPacketPtr packet) {
  if (!packet || !running_) {
    return;
  }

  JNIEnv* env = getJniEnv();
  if (!env) {
    SPDLOG_ERROR("Failed to get JNI environment in onIPPacket");
    return;
  }

  jbyteArray jpacket = nullptr;
  jclass cls = nullptr;

  try {
    const auto* raw_packet = packet->GetRawPacket();
    const void* data = static_cast<const void*>(raw_packet->getRawData());
    const auto len = raw_packet->getRawDataLen();

    if (!len || data == nullptr) {
      SPDLOG_ERROR("Serialized packet is empty");
      return;
    }

    jpacket = env->NewByteArray(len);
    if (!jpacket) {
      SPDLOG_ERROR("Failed to allocate jbyteArray");
      return;
    }

    env->SetByteArrayRegion(
        jpacket, 0, len, reinterpret_cast<const jbyte*>(data));
    if (env->ExceptionCheck()) {
      SPDLOG_ERROR("JNI Exception in SetByteArrayRegion");
      env->ExceptionDescribe();
      env->ExceptionClear();
      return;
    }

    cls = env->GetObjectClass(wrapper_);
    if (!cls) {
      SPDLOG_ERROR("Failed to get object class");
      return;
    }

    jmethodID on_message_impl = env->GetMethodID(cls, "onMessageImpl", "([B)V");
    if (!on_message_impl) {
      SPDLOG_ERROR("Failed to get method ID: onMessageImpl([B)V");
      return;
    }

    // Call java method
    env->CallVoidMethod(wrapper_, on_message_impl, jpacket);
    if (env->ExceptionCheck()) {
      SPDLOG_ERROR("JNI Exception in CallVoidMethod");
      env->ExceptionDescribe();
      env->ExceptionClear();
    }
  } catch (const std::exception& ex) {
    SPDLOG_ERROR("Exception in onIPPacket: {}", ex.what());
  } catch (...) {
    SPDLOG_ERROR("Unknown exception in onIPPacket");
  }

  // Clean up
  if (jpacket) {
    env->DeleteLocalRef(jpacket);
  }
  if (cls) {
    env->DeleteLocalRef(cls);
  }
}

void WrapperWebsocketClient::onConnectedCallback() {
  if (!running_.load()) {
    SPDLOG_WARN("onConnectedCallback called but client is not running");
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

bool WrapperWebsocketClient::Send(std::string pkt) {
  if (!running_) {
    return false;
  }
  try {
    std::vector<uint8_t> packet_data;
    packet_data.reserve(pkt.size());
    std::move(pkt.begin(), pkt.end(), std::back_inserter(packet_data));

    auto ip_packet = fptn::common::network::IPPacket::Parse(std::move(packet_data));
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
