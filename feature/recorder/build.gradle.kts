plugins {
    alias(libs.plugins.talkdraft.kmp.library)
    alias(libs.plugins.talkdraft.koin.config)
    alias(libs.plugins.mokkery)
}

kotlin {
    android {
        namespace = "com.sam.talkdraft.feature_recorder"
    }


    sourceSets {
        commonMain.dependencies {
            implementation(libs.bundles.kotlinx.common)
            implementation(libs.bundles.koin.common)
            implementation(libs.okio)

            implementation(project(":core:common"))
            implementation(project(":core:recorder"))
            implementation(project(":core:transcription-core"))
            implementation(project(":core:recorder-visualizer"))

            implementation(project(":feature:model-manager"))
            implementation(project(":feature:recordings"))
        }
        commonTest.dependencies {
            implementation(libs.bundles.testing.common)
            implementation(libs.turbine)

            implementation(project(":core:testing"))
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
