plugins {
    alias(libs.plugins.talkdraft.kmp.library)
    alias(libs.plugins.talkdraft.koin.config)
}

kotlin {
    android {
        namespace = "com.sam.talkdraft.recorder_visualizer"
    }


    sourceSets {
        commonMain.dependencies {
            implementation(libs.bundles.koin.common)
            implementation(libs.kotlinx.coroutines.core)
            implementation(project(":core:common"))
            implementation(project(":core:recorder"))
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
