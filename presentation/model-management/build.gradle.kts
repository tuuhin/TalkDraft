plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {

    jvmToolchain(25)

    applyDefaultHierarchyTemplate()

    android {
        namespace = "com.sam.talkdraft.model_management"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        androidResources {
            enable = true
        }
    }

    listOf(iosArm64(), iosSimulatorArm64())

    sourceSets {

        commonMain.dependencies {
            // compose ui & navigation
            implementation(libs.bundles.compose.ui)
            implementation(libs.bundles.compose.navigation3)
            // koin
            implementation(libs.bundles.koin.common)
            implementation(libs.bundles.koin.compose)
            // local
            implementation(project(":core:common"))
            implementation(project(":core:background-jobs"))
            implementation(project(":presentation:design-system"))
            implementation(project(":presentation:navigation"))
            implementation(project(":feature:model-manager"))
            implementation(project(":feature:model-downloader"))
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
    }

}
