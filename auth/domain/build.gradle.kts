plugins {
    id("org.fptn.vpn.library.kotlin")
    id("org.fptn.vpn.library.koin")
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.koin.core)
    implementation(libs.kotlinx.coroutines.core)
}
