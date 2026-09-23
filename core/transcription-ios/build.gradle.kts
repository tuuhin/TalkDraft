@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "com.sam.talkdraft.transcription.ios"

kotlin {

    jvmToolchain(25)

    applyDefaultHierarchyTemplate()

    // only ios targets
    listOf(iosArm64(), iosSimulatorArm64())

    swiftPMDependencies {
        packageResolvedSynchronization = noSynchronization()

        localSwiftPackage(
            directory = layout.projectDirectory.dir("src/transcription-native"),
            products = listOf(product("IosNativeTranscription")),
        )
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kermit)
        }
    }
}
