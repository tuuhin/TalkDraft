plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.androidLint)
    alias(libs.plugins.koin.compiler)
}

kotlin {
    jvmToolchain(25)

    android {
        namespace = "com.sam.talkdraft.remote_config"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    listOf(iosArm64(), iosSimulatorArm64())

    sourceSets {
        commonMain.dependencies {
            // local modules
            implementation(project(":core:common"))

            implementation(libs.bundles.koin.common)
            implementation(libs.posthog.kmp)
            implementation(libs.kotlinx.coroutines.core)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
