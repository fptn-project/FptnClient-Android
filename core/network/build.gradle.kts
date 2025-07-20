plugins {
    id("org.fptn.vpn.library.android")
    id("org.fptn.vpn.library.koin")
}

android {
    namespace = "org.fptn.vpn.core.network"
}

dependencies {
    implementation(libs.androidx.tracing)
    implementation(libs.koin.core)
    implementation(libs.kotlinx.coroutines.core)
}
