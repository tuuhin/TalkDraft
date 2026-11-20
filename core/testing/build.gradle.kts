plugins {
    alias(libs.plugins.talkdraft.kmp.library)
    alias(libs.plugins.talkdraft.koin.config)
}

kotlin {

    android {
        namespace = "com.sam.talkdraft.testing"

        androidResources {
            enable = true
        }
    }


    sourceSets {
        androidMain.dependencies {
            implementation(libs.junit)
            implementation(libs.bundles.testing.android)
        }
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.koin.annotations)
            api(libs.kotlinx.coroutines.test)
            api(libs.turbine)
            implementation(project(":core:common"))
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
