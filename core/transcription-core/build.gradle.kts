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
        namespace = "com.sam.talkdraft.transcriptions"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    listOf(iosArm64(), iosSimulatorArm64())

    sourceSets {
        androidMain.dependencies {
            implementation(project(":core:transcription-android"))
            implementation(libs.androidx.collection)
        }

        commonMain.dependencies {
            // local
            implementation(project(":core:common"))
            // koin
            implementation(libs.koin.core)
            api(libs.koin.annotations)
            implementation(libs.bundles.koin.common)
            implementation(libs.supabase.auth)
        }

        iosMain.dependencies {
            implementation(project(":core:transcription-ios"))
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
