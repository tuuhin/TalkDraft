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
        namespace = "com.sam.talkdraft.designsystem"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        androidResources {
            enable = true
        }
    }

    listOf(iosArm64(), iosSimulatorArm64())

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.downloadable.fonts)
        }
        commonMain.dependencies {
            // compose ui & navigation
            implementation(libs.bundles.compose.ui)
            implementation(libs.bundles.compose.navigation3)
            implementation(libs.cmp.adaptive)
            implementation(libs.cmp.ui.tooling.preview)
            implementation(libs.materialKolor)
            // logging & notifications
            implementation(libs.kermit)
        }
    }
}


compose.resources {
    publicResClass = false
    packageOfResClass = "com.sam.talkdraft.designs"
    generateResClass = auto

    customDirectory(
        "iosMain",
        provider {
            layout.projectDirectory.dir("src/iosMain/composeResources")
        },
    )
}

dependencies {
    add("androidRuntimeClasspath", libs.androidx.ui.tooling.preview)
}
