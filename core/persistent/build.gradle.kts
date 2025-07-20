plugins {
    id("org.fptn.vpn.library.android")
    id("org.fptn.vpn.library.koin")
}

android {
    namespace = "org.fptn.vpn.core.persistent"
}

dependencies {
    implementation(libs.datastore)
    implementation(libs.koin.core)
}
