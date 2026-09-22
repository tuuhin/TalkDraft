@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.koin.compiler)
}

kotlin {

    jvmToolchain(25)

    android {
        namespace = "com.sam.talkdraft.files_archive"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTest {
            isIncludeAndroidResources = true
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "com.sam.talkdraft.testing.InstrumentTestRunner"
            execution = "HOST"
        }
    }

    listOf(iosArm64(), iosSimulatorArm64())


    applyDefaultHierarchyTemplate()

    sourceSets {
        androidMain.dependencies {
            implementation(project(":core:archive-android"))
        }
        iosMain.dependencies {
            implementation(project(":core:archive-ios"))
        }
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(libs.kermit)
            implementation(libs.okio)
            implementation(libs.kmp.okio.zip)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.bundles.koin.common)
        }
        commonTest.dependencies {
            implementation(project(":core:testing"))
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
