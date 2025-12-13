/*=============================================================================
Copyright (c) 2024-2025 Stas Skokov
Copyright (c) 2024-2025 brightsunshine54

Distributed under the MIT License (https://opensource.org/licenses/MIT)
=============================================================================*/

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
