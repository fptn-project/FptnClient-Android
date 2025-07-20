plugins {
    id("org.fptn.vpn.library.android")
    id("org.fptn.vpn.library.koin")
}

android {
    namespace = "org.fptn.vpn.settings.ui"
}

dependencies {
    implementation(libs.androidx.navigation.runtime.android)
}
