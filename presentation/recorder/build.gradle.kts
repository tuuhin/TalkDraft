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
        namespace = "com.sam.talkdraft.recorder_ui"
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
            implementation(libs.cmp.adaptive)
            implementation(libs.cmp.ui.tooling.preview)
            // logging & notifications
            implementation(libs.kermit)
            implementation(libs.kotlinx.collections.immutable)
            // koin
            implementation(libs.bundles.koin.common)
            implementation(libs.bundles.koin.compose)
            // local
            implementation(project(":core:common"))
            implementation(project(":core:transcription-core"))
            implementation(project(":core:analytics"))
            implementation(project(":core:recorder"))
            implementation(project(":core:permissions"))
            implementation(project(":feature:recorder"))
            implementation(project(":presentation:design-system"))
            implementation(project(":presentation:navigation"))
        }
    }

    compilerOptions {
        optIn.add("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
    }

}
