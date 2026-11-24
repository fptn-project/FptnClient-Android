# FptnClient-Android (`feat/md3-compose-ui`)

Modern Material Design 3 Expressive Jetpack Compose frontend for the [FPTN](https://fptn.org) Android VPN client.

---

## Highlights of `feat/md3-compose-ui`

- **Pure Jetpack Compose & Material 3 Expressive**:
  - Replaces legacy View layouts with reactive Compose UI, Unidirectional Data Flow (UDF), and `UiState` / `StateFlow`.
  - 4 primary tabs: **Home** (status, greeting, telemetry, interactive wave visualizer), **Servers** (auto-select, token management, TCP ping test), **Stats** (weekly volume bar chart on Canvas, live speeds, process memory diagnostics), **Settings** (grouped MD3 Expressive cards).
- **Advanced Theme Foundation**:
  - Light, Dark, and Pure Black AMOLED (`#000000` background, elevated containers) display modes.
  - 3 Expressive accent palettes: Lilac / Lavender, Forest / Green, Ocean / Blue.
  - Multi-level contrast adjustments and selectable navigation bar styles (Floating Pill vs. Docked Bar).
- **Strict Architecture Boundaries**:
  - Core C++ engine (`app/src/main/cpp/`), Conan toolchain recipes, and JNI bridges remain intact.
  - High-performance packet-tunneling logic (`org.fptn.vpn.services.vpn.*`, `org.fptn.vpn.network.*`) preserved without modifications.
  - Seamless Android `VpnService.prepare` permission integration and Quick Settings tile synchronization.

---

## Setup & Toolchain

### Requirements
- **JDK**: Java 17 strictly enforced via Gradle toolchain (`JavaLanguageVersion.of(17)`).
- **Android SDK**: `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`.
- **NDK**: `26.1.10909125` / `28.1.13356709`.
- **Build System**: Android Gradle Plugin 8.13+, Conan 2.x, CMake, Ninja.

### Clone with Submodules
```bash
git clone --recursive <repository-url>
cd FptnClient-Android
```

### Conan Configuration
```bash
pip install conan numpy cmake ninja
conan profile detect -f
```

---

## Build Variants

Build FOSS flavor APK without proprietary services:
```bash
# Debug APK
./gradlew assembleFossDebug --stacktrace

# Release APK
./gradlew assembleFossRelease
```

---

## CI / CD Automation

The repository includes a dedicated GitHub Actions workflow: [`.github/workflows/android-foss-ci.yml`](.github/workflows/android-foss-ci.yml):
- Triggers on push to `feat/md3-compose-ui` and manual `workflow_dispatch`.
- Sets up JDK 17, Android NDK, Python 3.11, Conan, and build dependencies.
- Builds `assembleFossDebug` and uploads generated APK artifacts automatically.
