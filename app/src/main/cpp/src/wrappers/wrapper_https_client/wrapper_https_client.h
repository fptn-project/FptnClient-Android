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

#include <jni.h>

#include "fptn-protocol-lib/https/api_client/api_client.h"

namespace fptn::wrapper {

using fptn::protocol::https::ApiClient;
using fptn::protocol::https::Response;

class WrapperHttpsClient {
 public:
  WrapperHttpsClient(JNIEnv* env,
      jobject wrapper,
      std::string host,
      int port,
      std::string sni,
      std::string md5_fingerprint,
      fptn::protocol::https::CensorshipStrategy censorship_strategy);

  Response Get(const std::string& handle, int timeout = 10);

  Response Post(
      const std::string& handle, const std::string& request, int timeout = 10);

 private:
  const JNIEnv* env_;
  const jobject wrapper_;
  ApiClient https_client_;
};
}  // namespace fptn::wrapper
