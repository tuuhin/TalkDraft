plugins {
    alias(libs.plugins.talkdraft.kmp.library)
    alias(libs.plugins.androidLint)
    alias(libs.plugins.talkdraft.koin.config)
}

kotlin {

    android {
        namespace = "com.sam.talkdraft.transcriptions"
    }


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
