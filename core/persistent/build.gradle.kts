plugins {
    id("org.fptn.vpn.library.android")
    id("org.fptn.vpn.library.koin")
}

android {
    namespace = "org.fptn.vpn.core.persistent"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.datastore)
    implementation(libs.koin.android)
    implementation(libs.koin.core)

    ksp(libs.androidx.room.compiler)
}
