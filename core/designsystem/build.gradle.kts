plugins {
    id("org.fptn.vpn.library.android")
    id("org.fptn.vpn.library.android.compose")
}

android {
    namespace = "org.fptn.vpn.core.designsystem"
}

dependencies {
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.test.manifest)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
