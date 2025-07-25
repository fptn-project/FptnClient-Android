plugins {
    id("org.fptn.vpn.library.android")
    id("org.fptn.vpn.library.koin")
}

android {
    namespace = "org.fptn.vpn.auth.data"
}

dependencies {
    implementation(project(":auth:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:persistent"))
    implementation(libs.koin.annotations.jvm)
    implementation(libs.koin.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.mockk)
}
