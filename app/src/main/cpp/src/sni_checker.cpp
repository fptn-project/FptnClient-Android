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
#include <memory>
#include <string>

#include "fptn-protocol-lib/https/api_client/api_client.h"
#include "wrappers/utils/utils.h"

class NativeSniChecker {
 public:
  NativeSniChecker(const std::string& host,
      int port,
      const std::string& md5_fingerprint,
      const std::string& censorship_strategy)
      : host_(host), port_(port), md5_fingerprint_(md5_fingerprint),
        strategy_(fptn::protocol::https::CensorshipStrategy::kSni) {
    if (censorship_strategy == "OBFUSCATION") {
      strategy_ = fptn::protocol::https::CensorshipStrategy::kTlsObfuscator;
    } else if (censorship_strategy == "SNI-REALITY") {
      strategy_ = fptn::protocol::https::CensorshipStrategy::kSniRealityMode;
    } else if (censorship_strategy == "SNI-REALITY-CHROME-149") {
      strategy_ = fptn::protocol::https::CensorshipStrategy::kSniRealityModeChrome149;
    } else if (censorship_strategy == "SNI-REALITY-CHROME-148") {
      strategy_ = fptn::protocol::https::CensorshipStrategy::kSniRealityModeChrome148;
    } else if (censorship_strategy == "SNI-REALITY-CHROME-147") {
      strategy_ = fptn::protocol::https::CensorshipStrategy::kSniRealityModeChrome147;
    } else if (censorship_strategy == "SNI-REALITY-CHROME-146") {
      strategy_ = fptn::protocol::https::CensorshipStrategy::kSniRealityModeChrome146;
    } else if (censorship_strategy == "SNI-REALITY-CHROME-145") {
      strategy_ = fptn::protocol::https::CensorshipStrategy::kSniRealityModeChrome145;
    } else if (censorship_strategy == "SNI-REALITY-FIREFOX-151") {
      strategy_ = fptn::protocol::https::CensorshipStrategy::kSniRealityModeFirefox151;
    } else if (censorship_strategy == "SNI-REALITY-FIREFOX-150") {
      strategy_ = fptn::protocol::https::CensorshipStrategy::kSniRealityModeFirefox150;
    } else if (censorship_strategy == "SNI-REALITY-FIREFOX-149") {
      strategy_ = fptn::protocol::https::CensorshipStrategy::kSniRealityModeFirefox149;
    } else if (censorship_strategy == "SNI-REALITY-YANDEX-26-4") {
      strategy_ = fptn::protocol::https::CensorshipStrategy::kSniRealityModeYandex26_4;
    } else if (censorship_strategy == "SNI-REALITY-YANDEX-26-3") {
      strategy_ = fptn::protocol::https::CensorshipStrategy::kSniRealityModeYandex26_3;
    } else if (censorship_strategy == "SNI-REALITY-YANDEX-25") {
      strategy_ = fptn::protocol::https::CensorshipStrategy::kSniRealityModeYandex25;
    } else if (censorship_strategy == "SNI-REALITY-YANDEX-24") {
      strategy_ = fptn::protocol::https::CensorshipStrategy::kSniRealityModeYandex24;
    } else if (censorship_strategy == "SNI-REALITY-SAFARI-26-5") {
      strategy_ = fptn::protocol::https::CensorshipStrategy::kSniRealityModeSafari26_5;
    } else if (censorship_strategy == "SNI-REALITY-SAFARI-26-4") {
      strategy_ = fptn::protocol::https::CensorshipStrategy::kSniRealityModeSafari26_4;
    }
    SPDLOG_INFO("NativeSniChecker created for {}:{} [strategy={}]", host, port,
        static_cast<int>(strategy_));
  }

  bool checkSni(const std::string& sni) {
    SPDLOG_INFO("=== Starting SNI check for: {} ===", sni);

    try {
      // Step 1: Test handshake
      SPDLOG_INFO("Step 1/2: Testing handshake for SNI: {}", sni);

      fptn::protocol::https::ApiClient client(host_, port_, sni,
          md5_fingerprint_, strategy_);

      const auto handshake_start = std::chrono::steady_clock::now();
      const bool handshake_success = client.TestHandshake(10);
      const auto handshake_end = std::chrono::steady_clock::now();
      const auto handshake_ms =
          std::chrono::duration_cast<std::chrono::milliseconds>(
              handshake_end - handshake_start)
              .count();

      if (!handshake_success) {
        SPDLOG_WARN("Step 1/2 FAILED: Handshake failed for SNI: {} after {} ms",
            sni, handshake_ms);
        return false;
      }
      SPDLOG_INFO("Step 1/2 SUCCESS: Handshake completed for SNI: {} in {} ms",
          sni, handshake_ms);

      // Step 2: Download test file
      SPDLOG_INFO("Step 2/2: Downloading test file for SNI: {}", sni);

      auto download_start = std::chrono::steady_clock::now();

      fptn::protocol::https::ApiClient download_client(host_, port_, sni,
          md5_fingerprint_, strategy_);

      const auto response = download_client.Get("/api/v1/test/file.bin", 15);

      const auto download_end = std::chrono::steady_clock::now();
      const auto download_ms =
          std::chrono::duration_cast<std::chrono::milliseconds>(
              download_end - download_start)
              .count();

      if (response.code == 200) {
        SPDLOG_INFO(
            "Step 2/2 SUCCESS: File downloaded for SNI: {} in {} ms, size: {} "
            "bytes",
            sni, download_ms, response.body.size());
        SPDLOG_INFO("=== SNI check COMPLETED SUCCESSFULLY for: {} ===", sni);
        return true;
      } else {
        SPDLOG_WARN(
            "Step 2/2 FAILED: Download failed for SNI: {} - HTTP code: {}, "
            "error: {}",
            sni, response.code, response.errmsg);
        SPDLOG_INFO("=== SNI check FAILED for: {} (download error) ===", sni);
        return false;
      }

    } catch (const std::exception& e) {
      SPDLOG_ERROR("=== SNI check EXCEPTION for {}: {} ===", sni, e.what());
      return false;
    }
  }

 private:
  const std::string host_;
  const int port_;
  const std::string md5_fingerprint_;
  fptn::protocol::https::CensorshipStrategy strategy_;
};

extern "C" {

JNIEXPORT jlong JNICALL
Java_org_fptn_vpn_services_snichecker_SniChecker_nativeCreate(JNIEnv* env,
    jobject thiz,
    jstring host_param,
    jint port_param,
    jstring md5_fingerprint_param,
    jstring censorship_strategy_param) {
  fptn::wrapper::init_logger();

  const char* host_str = env->GetStringUTFChars(host_param, nullptr);
  const char* md5_str = env->GetStringUTFChars(md5_fingerprint_param, nullptr);
  const char* strategy_str =
      env->GetStringUTFChars(censorship_strategy_param, nullptr);

  auto* checker = new NativeSniChecker(std::string(host_str), (int)port_param,
      std::string(md5_str ? md5_str : ""), std::string(strategy_str));

  env->ReleaseStringUTFChars(host_param, host_str);
  env->ReleaseStringUTFChars(md5_fingerprint_param, md5_str);
  env->ReleaseStringUTFChars(censorship_strategy_param, strategy_str);

  return reinterpret_cast<jlong>(checker);
}

JNIEXPORT jboolean JNICALL
Java_org_fptn_vpn_services_snichecker_SniChecker_nativeCheckSni(
    JNIEnv* env, jobject thiz, jlong native_handle, jstring sni_param) {
  auto* checker = reinterpret_cast<NativeSniChecker*>(native_handle);
  if (!checker) {
    SPDLOG_ERROR("NativeSniChecker handle is null");
    return JNI_FALSE;
  }

  const char* sni_str = env->GetStringUTFChars(sni_param, nullptr);
  bool result = checker->checkSni(std::string(sni_str));
  env->ReleaseStringUTFChars(sni_param, sni_str);

  return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_org_fptn_vpn_services_snichecker_SniChecker_nativeDestroy(
    JNIEnv* env, jobject thiz, jlong native_handle) {
  auto* checker = reinterpret_cast<NativeSniChecker*>(native_handle);
  delete checker;
}

}  // extern "C"
