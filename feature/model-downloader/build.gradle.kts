plugins {
    alias(libs.plugins.talkdraft.kmp.library)
    alias(libs.plugins.talkdraft.koin.config)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    android {
        namespace = "com.sam.talkdraft.model_downloader"
    }


    sourceSets {
        commonMain.dependencies {
            implementation(libs.bundles.koin.common)
            implementation(libs.okio)
            implementation(libs.kotlinx.io.okio)
            implementation(ktorLibs.client.core)
            // local
            implementation(project(":feature:model-manager"))
            implementation(project(":core:common"))
            implementation(project(":core:archive-core"))
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
