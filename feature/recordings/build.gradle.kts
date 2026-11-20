plugins {
    alias(libs.plugins.talkdraft.kmp.library)
    alias(libs.plugins.talkdraft.koin.config)
    alias(libs.plugins.mokkery)
}

kotlin {
    android {
        namespace = "com.sam.talkdraft.feature_recordings"
    }


    sourceSets {
        commonMain.dependencies {
            implementation(libs.bundles.kotlinx.common)
            implementation(libs.bundles.koin.common)
            implementation(libs.okio)
            // local
            implementation(project(":core:common"))
            implementation(project(":core:database"))
            implementation(project(":core:player"))
            // feature
            implementation(project(":feature:model-manager"))
        }
        commonTest.dependencies {
            implementation(project(":core:testing"))
            implementation(libs.bundles.testing.common)
            implementation(libs.turbine)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
