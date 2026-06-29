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


#pragma once

#include <chrono>
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

  bool Send(fptn::common::network::IPPacketData pkt);

  const fptn::common::network::IPv4Address& IPv4Address() const;
  const fptn::common::network::IPv6Address& IPv6Address() const;

 protected:
  void Run();

  void onIPPacket(fptn::common::network::IPPacketPtr);

  void onConnectedCallback();

  void onIpAssignedCallback(const fptn::common::network::IPv4Address& ipv4,
                            const fptn::common::network::IPv6Address& ipv6);

 private:
  const int kMaxReconnectionAttempts_ = 3;

  std::thread th_;
  mutable std::mutex mutex_;
  mutable std::atomic<bool> running_;
  mutable std::atomic<int> reconnection_attempts_;
  std::chrono::steady_clock::time_point window_start_time_;
  // onOpenImpl must fire only once per object lifetime — set on first successful
  // connect, never cleared until this object is destroyed and a new one is created.
  std::atomic<bool> has_opened_{false};

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

  fptn::common::network::IPv4Address ipv4_address_;
  fptn::common::network::IPv6Address ipv6_address_;
};
}  // namespace fptn::wrapper
