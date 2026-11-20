plugins {
    alias(libs.plugins.talkdraft.kmp.library)
    alias(libs.plugins.talkdraft.koin.config)
}

kotlin {
    android {
        namespace = "com.sam.talkdraft.remote_config"
    }


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
