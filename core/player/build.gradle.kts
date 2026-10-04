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
        namespace = "com.sam.talkdraft.player"
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

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.compilations.getByName("main").cinterops.create("nsExtras") {
            definitionFile = project.file("src/nativeInterop/cinterop/NSkeyObserver.def")
            packageName = "com.sam.talkdraft.platform.kvo"
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.exoplayer.dash)
            implementation(libs.androidx.media3.inspector)
            implementation(libs.androidx.concurrent)
        }
        commonMain.dependencies {
            implementation(libs.bundles.koin.common)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.kotlinx.datetime)
            // local
            implementation(project(":core:datastore"))
            implementation(project(":core:common"))
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.bundles.testing.android)
            implementation(libs.androidx.media3.testing)
            implementation(project(":core:testing"))
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
