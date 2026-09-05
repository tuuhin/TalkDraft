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
        namespace = "com.sam.talkdraft.background_jobs"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    listOf(iosArm64(), iosSimulatorArm64())

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.work.runtime.ktx)
            implementation(project(":core:worker-android"))
        }

        commonMain.dependencies {
            // koin
            implementation(libs.bundles.koin.common)

            implementation(project(":core:common"))
            implementation(project(":feature:model-downloader"))
            implementation(project(":feature:model-manager"))
        }

        iosMain.dependencies {
            // invoking background jobs directly from here so analytics
            implementation(project(":core:analytics"))
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
