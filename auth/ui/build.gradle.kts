plugins {
    id("org.fptn.vpn.library.android")
    id("org.fptn.vpn.library.koin")
    id("org.fptn.vpn.library.android.compose")
}

android {
    namespace = "org.fptn.vpn.auth.ui"
}

dependencies {

    implementation(project(":auth:data"))
    implementation(project(":auth:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.core)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
