plugins {
    `kotlin-dsl`
}

buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(libs.android.gradle.plugin)
    implementation(libs.detekt)
    implementation(libs.java.poet)
    implementation(libs.guava)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.metadata.jvm)
    implementation(libs.ksp.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "org.fptn.vpn.application"
            implementationClass = "org.fptn.vpn.gradle.AndroidApplicationConventionPlugin"
        }
        register("androidApplicationCompose") {
            id = "org.fptn.vpn.application.compose"
            implementationClass = "org.fptn.vpn.gradle.AndroidApplicationComposeConventionPlugin"
        }
        register("androidApplicationKoin") {
            id = "org.fptn.vpn.application.koin"
            implementationClass = "org.fptn.vpn.gradle.AndroidApplicationKoinConventionPlugin"
        }
        register("androidLibrary") {
            id = "org.fptn.vpn.library.android"
            implementationClass = "org.fptn.vpn.gradle.AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "org.fptn.vpn.library.android.compose"
            implementationClass = "org.fptn.vpn.gradle.AndroidLibraryComposeConventionPlugin"
        }
        register("kotlinLibrary") {
            id = "org.fptn.vpn.library.kotlin"
            implementationClass = "org.fptn.vpn.gradle.KotlinLibraryConventionPlugin"
        }
        register("koinLibrary") {
            id = "org.fptn.vpn.library.koin"
            implementationClass = "org.fptn.vpn.gradle.KoinConventionPlugin"
        }
    }
}
