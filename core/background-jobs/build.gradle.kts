plugins {
    alias(libs.plugins.talkdraft.kmp.library)
    alias(libs.plugins.talkdraft.koin.config)
}

kotlin {
    android {
        namespace = "com.sam.talkdraft.background_jobs"
    }


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
