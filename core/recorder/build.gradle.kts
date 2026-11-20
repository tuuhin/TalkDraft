plugins {
    alias(libs.plugins.talkdraft.kmp.library)
    alias(libs.plugins.talkdraft.koin.config)
}

kotlin {
    android {
        namespace = "com.sam.talkdraft.recorder"
        androidResources {
            enable = true
        }
    }


    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.media3.common)
        }
        commonMain.dependencies {
            implementation(libs.bundles.koin.common)
            implementation(project(":core:common"))
            implementation(libs.okio)
        }
        commonTest.dependencies {
            implementation(libs.koin.test)
            implementation(project(":core:testing"))
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.bundles.testing.android)
            implementation(libs.androidx.rules)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

koinCompiler {
    userLogs = true
}
