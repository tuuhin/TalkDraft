plugins {
    alias(libs.plugins.talkdraft.kmp.library)
    alias(libs.plugins.talkdraft.koin.config)
}

kotlin {
    android {
        namespace = "com.sam.talkdraft.feature_onboarding"
    }


    sourceSets {
        commonMain.dependencies {
            implementation(libs.bundles.koin.common)
            implementation(libs.androidx.datastore.preferences)
            // local
            implementation(project(":core:datastore"))
            implementation(project(":core:common"))
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
