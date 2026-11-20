plugins {
    alias(libs.plugins.talkdraft.kmp.library)
    alias(libs.plugins.androidLint)
    alias(libs.plugins.talkdraft.koin.config)
}

kotlin {
    android {
        namespace = "com.sam.talkdraft.platform_capability"
    }


    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
        }

        commonMain.dependencies {
            // koin
            implementation(libs.koin.core)
            api(libs.koin.annotations)

            implementation(project(":core:common"))
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
