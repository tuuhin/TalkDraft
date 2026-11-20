plugins {
    alias(libs.plugins.talkdraft.compose.multiplatform)
    alias(libs.plugins.talkdraft.koin.config)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {

    jvmToolchain(25)

    applyDefaultHierarchyTemplate()

    android {
        namespace = "com.sam.talkdraft.home"
    }


    sourceSets {

        commonMain.dependencies {
            // compose ui & navigation
            implementation(libs.bundles.compose.ui)
            implementation(libs.bundles.compose.navigation3)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.cmp.adaptive)
            implementation(libs.cmp.ui.tooling.preview)
            // logging & notifications
            implementation(libs.kermit)
            // koin
            implementation(libs.bundles.koin.common)
            implementation(libs.bundles.koin.compose)
            // local
            implementation(project(":core:common"))
            implementation(project(":feature:recordings"))
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
