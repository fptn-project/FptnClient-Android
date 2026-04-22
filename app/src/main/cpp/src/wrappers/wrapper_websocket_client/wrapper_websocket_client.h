/*=============================================================================
Copyright (c) 2024-2025 Stas Skokov
Copyright (c) 2024-2025 brightsunshine54

Distributed under the MIT License (https://opensource.org/licenses/MIT)
=============================================================================*/

#pragma once

#include <jni.h>

#define FPTN_IP_ADDRESS_WITHOUT_PCAP
#include "fptn-protocol-lib/https/websocket_client/websocket_client.h"

namespace fptn::wrapper {

class WrapperWebsocketClient final {
 public:
  explicit WrapperWebsocketClient(jobject wrapper,
      std::string server_ip,
      int server_port,
      std::string tun_ipv4,
      std::string tun_ipv6,
      std::string sni,
      std::string access_token,
      std::string expected_md5_fingerprint,
      fptn::protocol::https::CensorshipStrategy censorship_strategy);

  ~WrapperWebsocketClient();

  bool Start();

  bool Stop();

  bool IsStarted();

  bool Send(std::string pkt);

 protected:
  void Run();

  void onIPPacket(fptn::common::network::IPPacketPtr);

  void onConnectedCallback();

 private:
  const int kMaxReconnectionAttempts_ = 35;

  std::thread th_;
  mutable std::mutex mutex_;
  mutable std::atomic<bool> running_;
  mutable std::atomic<int> reconnection_attempts_;

  const jobject wrapper_;

  const std::string server_ip_;
  const int server_port_;
  const std::string tun_ipv4_;
  const std::string tun_ipv6_;
  const std::string sni_;
  const std::string access_token_;
  const std::string expected_md5_fingerprint_;
  const fptn::protocol::https::CensorshipStrategy censorship_strategy_;

  fptn::protocol::https::WebsocketClientSPtr client_;
};
}  // namespace fptn::wrapper
