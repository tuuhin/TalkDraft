plugins {
    alias(libs.plugins.talkdraft.kmp.library)
    alias(libs.plugins.talkdraft.koin.config)
}

kotlin {
    android {
        namespace = "com.sam.talkdraft.files_archive"
    }



    applyDefaultHierarchyTemplate()

    sourceSets {
        androidMain.dependencies {
            implementation(project(":core:archive-android"))
        }
        iosMain.dependencies {
            implementation(project(":core:archive-ios"))
        }
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(libs.kermit)
            implementation(libs.okio)
            implementation(libs.kmp.okio.zip)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.bundles.koin.common)
        }
        commonTest.dependencies {
            implementation(project(":core:testing"))
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
