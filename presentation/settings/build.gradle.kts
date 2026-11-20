plugins {
    alias(libs.plugins.talkdraft.compose.multiplatform)
    alias(libs.plugins.talkdraft.koin.config)
}

kotlin {

    android {
        namespace = "com.sam.talkdraft.presentation.settings"

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
            implementation(libs.kotlinx.coroutines.core)
            // koin
            implementation(libs.bundles.koin.common)
            implementation(libs.bundles.koin.compose)
            // local
            implementation(project(":presentation:design-system"))
            implementation(project(":presentation:navigation"))
        }
    }

    compilerOptions {
        optIn.add("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
    }

}

dependencies {
    androidRuntimeClasspath(libs.cmp.ui.tooling)
}
