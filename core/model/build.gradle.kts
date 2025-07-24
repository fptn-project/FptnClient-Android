plugins {
    id("org.fptn.vpn.library.kotlin")
    alias(libs.plugins.serialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.core.jvm)
}
