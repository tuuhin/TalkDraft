plugins {
    alias(libs.plugins.talkdraft.compose.multiplatform)
    alias(libs.plugins.talkdraft.koin.config)
}

kotlin {
    android {
        namespace = "com.sam.talkdraft.onboarding"

        androidResources {
            enable = true
        }
    }


    sourceSets {

        commonMain.dependencies {
            // compose ui & navigation
            implementation(libs.bundles.compose.ui)
            implementation(libs.bundles.compose.navigation3)
            // kotlin
            implementation(libs.kotlinx.collections.immutable)
            // koin
            implementation(libs.bundles.koin.common)
            implementation(libs.bundles.koin.compose)
            // local
            implementation(project(":core:common"))
            implementation(project(":core:analytics"))
            implementation(project(":core:permissions"))
            implementation(project(":presentation:design-system"))
            implementation(project(":presentation:navigation"))
            implementation(project(":feature:onboarding"))
            implementation(project(":feature:model-manager"))
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
    }

}

dependencies {
    androidRuntimeClasspath(libs.cmp.ui.tooling)
}
