/*=============================================================================
Copyright (c) 2024-2025 Stas Skokov
Copyright (c) 2024-2025 brightsunshine54

Distributed under the MIT License (https://opensource.org/licenses/MIT)
=============================================================================*/

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
      std::string md5_fingerprint);

  Response Get(const std::string& handle, int timeout = 5);

  Response Post(
      const std::string& handle, const std::string& request, int timeout = 5);

 private:
  const JNIEnv* env_;
  const jobject wrapper_;
  ApiClient https_client_;
};
}  // namespace fptn::wrapper
