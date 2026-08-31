plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.androidLint)
    alias(libs.plugins.koin.compiler)
}

kotlin {

    jvmToolchain(25)

    applyDefaultHierarchyTemplate()

    android {
        namespace = "com.sam.talkdraft.permissions"
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

        androidResources {
            enable = true
        }
    }

    listOf(iosArm64(), iosSimulatorArm64())

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(libs.bundles.koin.common)
            // local
            implementation(project(":core:common"))
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.bundles.testing.android)
            implementation(project(":core:testing"))
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
