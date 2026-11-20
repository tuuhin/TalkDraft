plugins {
    alias(libs.plugins.talkdraft.kmp.library)
    alias(libs.plugins.talkdraft.koin.config)
}

kotlin {

    android {
        namespace = "com.sam.talkdraft.permissions"
        androidResources {
            enable = true
        }
    }


    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(libs.bundles.koin.common)
            // local
            implementation(project(":core:common"))
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.bundles.testing.android)
            implementation(project(":core:testing"))
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
