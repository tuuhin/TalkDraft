plugins {
    alias(libs.plugins.talkdraft.compose.multiplatform)
    alias(libs.plugins.talkdraft.koin.config)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    android {
        namespace = "com.sam.talkdraft.navigation"

        androidResources {
            enable = true
        }
    }


    sourceSets {

        commonMain.dependencies {
            // compose ui & navigation
            implementation(libs.bundles.compose.ui)
            implementation(libs.cmp.adaptive)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.bundles.compose.navigation3)
            implementation(libs.cmp.ui.tooling.preview)
            implementation(libs.kotlinx.collections.immutable)
            // logging & notifications
            implementation(libs.kermit)
            // koin
            implementation(libs.bundles.koin.common)
            implementation(libs.bundles.koin.compose)
            // local
            implementation(project(":core:common"))
            implementation(project(":presentation:design-system"))
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

dependencies {
    add("androidRuntimeClasspath", libs.androidx.ui.tooling.preview)
}
