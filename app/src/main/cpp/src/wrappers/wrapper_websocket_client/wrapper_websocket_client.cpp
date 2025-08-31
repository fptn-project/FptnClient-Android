/*=============================================================================
Copyright (c) 2024-2025 Stas Skokov
Copyright (c) 2024-2025 brightsunshine54

Distributed under the MIT License (https://opensource.org/licenses/MIT)
=============================================================================*/

#include "wrapper_websocket_client.h"

#include <jni.h>

#include "jnienv/jnienv.h"

#ifndef FPTN_CLIENT_DEFAULT_ADDRESS_IP6
#define FPTN_CLIENT_DEFAULT_ADDRESS_IP6 "fd00::1"
#endif

using fptn::protocol::websocket::WebsocketClient;
using fptn::wrapper::WrapperWebsocketClient;

WrapperWebsocketClient::WrapperWebsocketClient(jobject wrapper,
    std::string server_ip,
    int server_port,
    std::string tun_ipv4,
    std::string sni,
    std::string access_token,
    std::string expected_md5_fingerprint)
    : running_(false),
      reconnection_attempts_(kMaxReconnectionAttempts_),
      wrapper_(std::move(wrapper)),
      server_ip_(std::move(server_ip)),
      server_port_(server_port),
      tun_ipv4_(std::move(tun_ipv4)),
      sni_(std::move(sni)),
      access_token_(std::move(access_token)),
      expected_md5_fingerprint_(std::move(expected_md5_fingerprint)) {
  (void)wrapper_;
}

WrapperWebsocketClient::~WrapperWebsocketClient() { Stop(); }

bool WrapperWebsocketClient::Start() {
  const std::unique_lock<std::mutex> lock(mutex_);  // mutex

  running_ = true;
  th_ = std::thread(&WrapperWebsocketClient::Run, this);
  return th_.joinable();
}

bool WrapperWebsocketClient::Stop() {
  if (!running_) {
    return false;
  }

  const std::unique_lock<std::mutex> lock(mutex_);  // mutex

  // cppcheck-suppress identicalConditionAfterEarlyExit
  if (!running_) {  // Double-check after acquiring lock
    return false;
  }

  running_ = false;
  if (client_) {
    client_->Stop();
    client_.reset();
  }

  if (th_.joinable()) {
    th_.join();
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
  constexpr auto kReconnectionDelay = std::chrono::milliseconds(300);

  // Current count of reconnection attempts
  reconnection_attempts_ = kMaxReconnectionAttempts_;
  auto window_start_time = std::chrono::steady_clock::now();

  while (running_ && reconnection_attempts_) {
    try {
      {
        const std::unique_lock<std::mutex> lock(mutex_);  // mutex

        client_ = std::make_shared<WebsocketClient>(
            fptn::common::network::IPv4Address::Create(server_ip_),
            server_port_,
            fptn::common::network::IPv4Address::Create(tun_ipv4_),
            fptn::common::network::IPv6Address::Create(FPTN_CLIENT_DEFAULT_ADDRESS_IP6),
            std::bind(&WrapperWebsocketClient::onIPPacket, this,
                std::placeholders::_1),
            sni_, access_token_, expected_md5_fingerprint_,
            [this]() { onConnectedCallback(); });
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
    auto current_time = std::chrono::steady_clock::now();
    auto elapsed = current_time - window_start_time;
    // Reconnection attempt counting logic
    if (elapsed >= kReconnectionWindow) {
      // Reset counter if we're past the time window
      reconnection_attempts_ = kMaxReconnectionAttempts_;
      window_start_time = current_time;
    } else {
      // Decrement counter if within time window
      --reconnection_attempts_;
    }

    // Log connection failure and wait before retrying
    SPDLOG_ERROR(
        "Connection closed (attempt {}/{} in current window). Reconnecting in "
        "{}ms...",
        kMaxReconnectionAttempts_ - reconnection_attempts_,
        kMaxReconnectionAttempts_, kReconnectionDelay.count());
    std::this_thread::sleep_for(kReconnectionDelay);
  }

  // Final failure handler
  JNIEnv* env = nullptr;
  jclass cls = nullptr;
  try {
    if (running_ && !reconnection_attempts_) {
      SPDLOG_ERROR("Failed to establish connection after {} attempts",
          kMaxReconnectionAttempts_);
      env = getJniEnv();
      if (!env) {
        throw std::runtime_error("JNIEnv is null in final failure block");
      }

      cls = env->GetObjectClass(wrapper_);
      if (!cls) {
        throw std::runtime_error("Failed to get Java class from wrapper_");
      }

      jmethodID on_failure_impl = env->GetMethodID(cls, "onFailureImpl", "()V");
      if (!on_failure_impl) {
        throw std::runtime_error(
            "Failed to find method ID for onFailureImpl()");
        return;
      }

      env->CallVoidMethod(wrapper_, on_failure_impl);
      if (env->ExceptionCheck()) {
        SPDLOG_ERROR("JNI Exception in CallVoidMethod(onFailureImpl)");
        env->ExceptionDescribe();
        env->ExceptionClear();
      }
    }
  } catch (const std::exception& e) {
    SPDLOG_ERROR("Caught std::exception: {}", e.what());
  } catch (...) {
    SPDLOG_ERROR("Caught unknown exception");
  }
  if (env && cls) {
    env->DeleteLocalRef(cls);
  }
}

void WrapperWebsocketClient::onIPPacket(
    fptn::common::network::IPPacketPtr packet) {
  if (!packet || !running_) {
    return;
  }

  JNIEnv* env = nullptr;
  jbyteArray jpacket = nullptr;
  jclass cls = nullptr;
  try {
    env = getJniEnv();
    if (!env) {
      throw std::runtime_error("Failed to get JNI environment");
    }

    const auto* raw_packet = packet->GetRawPacket();
    const void* data = static_cast<const void*>(raw_packet->getRawData());
    const auto len = raw_packet->getRawDataLen();

    if (!len || data == nullptr) {
      throw std::runtime_error("Serialized packet is empty");
    }

    jpacket = env->NewByteArray(len);
    if (!jpacket) {
      throw std::runtime_error("Failed to allocate jbyteArray");
    }

    env->SetByteArrayRegion(jpacket, 0, len, reinterpret_cast<const jbyte*>(data));
    if (env->ExceptionCheck()) {
      env->ExceptionDescribe();
      env->ExceptionClear();
      throw std::runtime_error("JNI Exception in SetByteArrayRegion");
    }

    cls = env->GetObjectClass(wrapper_);
    if (!cls) {
      throw std::runtime_error("Failed to get object class");
    }

    jmethodID on_message_impl = env->GetMethodID(cls, "onMessageImpl", "([B)V");
    if (!on_message_impl) {
      throw std::runtime_error("Failed to get method ID: onMessageImpl([B)V");
    }
    // Call java method
    if (running_) {
      env->CallVoidMethod(wrapper_, on_message_impl, jpacket);
      if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        throw std::runtime_error("JNI Exception in CallVoidMethod");
      }
    }
  } catch (const std::exception& ex) {
    SPDLOG_ERROR("Exception in onIPPacket: {}", ex.what());
  } catch (...) {
    SPDLOG_ERROR("Unknown exception in onIPPacket");
  }

  // Clean up
  if (env && jpacket) {
    env->DeleteLocalRef(jpacket);
  }
  if (env && cls) {
    env->DeleteLocalRef(cls);
  }
}

void WrapperWebsocketClient::onConnectedCallback() {
  if (!running_) {
    SPDLOG_WARN("onConnectedCallback called but client is not running");
    return;
  }

  JNIEnv* env = nullptr;
  jclass cls = nullptr;

  try {
    env = getJniEnv();
    if (!env) {
      throw std::runtime_error("Failed to get JNI environment");
    }

    cls = env->GetObjectClass(wrapper_);
    if (!cls) {
      throw std::runtime_error("Failed to get Java class from wrapper_");
    }

    jmethodID on_open_impl = env->GetMethodID(cls, "onOpenImpl", "()V");
    if (!on_open_impl) {
      throw std::runtime_error("Failed to find method ID for onOpenImpl()");
    }

    env->CallVoidMethod(wrapper_, on_open_impl);
    if (env->ExceptionCheck()) {
      env->ExceptionDescribe();
      env->ExceptionClear();
      throw std::runtime_error(
          "JNI Exception in CallVoidMethod for onOpenImpl()");
    }
  } catch (const std::exception& ex) {
    SPDLOG_ERROR("Exception in onConnectedCallback: {}", ex.what());
  } catch (...) {
    SPDLOG_ERROR("Unknown exception in onConnectedCallback");
  }
  // Cleanup
  if (env && cls) {
    env->DeleteLocalRef(cls);
  }
}

bool WrapperWebsocketClient::Send(std::string pkt) {
  auto ip_packet = fptn::common::network::IPPacket::Parse(std::move(pkt));
  if (ip_packet && running_) {
    const std::unique_lock<std::mutex> lock(mutex_);  // mutex

    if (running_ && client_ && client_->IsStarted()) {
      client_->Send(std::move(ip_packet));
      return true;
    }
  }
  return false;
}
