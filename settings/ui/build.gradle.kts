plugins {
    id("org.fptn.vpn.library.android")
}

android {
    namespace = "org.fptn.vpn.settings.ui"
}

dependencies {
    implementation(libs.androidx.navigation.runtime.android)
}
