plugins {
    alias(libs.plugins.talkdraft.kmp.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {

    android {
        namespace = "com.sam.talkdraft.user_management"

        androidResources {
            enable = true
        }
    }


    sourceSets {

        commonMain.dependencies {
            // kotlin
            implementation(libs.kotlinx.collections.immutable)
            // koin
            implementation(libs.bundles.koin.common)
            // local
            implementation(project(":core:common"))
            implementation(project(":core:analytics"))
            implementation(project(":core:auth"))
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
