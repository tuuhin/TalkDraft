plugins {
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.koin.compiler)
}

kotlin {
    jvmToolchain(25)

    android {
        namespace = "com.sam.talkdraft.testing"
        minSdk = libs.versions.android.minSdk.get().toInt()
        compileSdk = libs.versions.android.compileSdk.get().toInt()

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

    sourceSets {
        androidMain.dependencies {
            implementation(libs.junit)
            implementation(libs.bundles.testing.android)
        }
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.koin.annotations)
            api(libs.kotlinx.coroutines.test)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
