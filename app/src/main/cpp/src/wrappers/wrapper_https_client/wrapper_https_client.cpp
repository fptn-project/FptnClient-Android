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


#include "wrapper_https_client.h"

using fptn::protocol::https::ApiClient;
using fptn::protocol::https::Response;

namespace fptn::wrapper {

WrapperHttpsClient::WrapperHttpsClient(JNIEnv* env,
    jobject wrapper,
    std::string host,
    int port,
    std::string sni,
    std::string md5_fingerprint,
    fptn::protocol::https::CensorshipStrategy censorship_strategy)
    : env_(env),
      wrapper_(std::move(wrapper)),
      https_client_(
          std::move(host), port, std::move(sni), std::move(md5_fingerprint), censorship_strategy) {
}

Response WrapperHttpsClient::Get(const std::string& handle, int timeout) {
  return https_client_.Get(handle, timeout);
}

Response WrapperHttpsClient::Post(
    const std::string& handle, const std::string& request, int timeout) {
    return https_client_.Post(handle, request, "application/json", timeout);
}
}
