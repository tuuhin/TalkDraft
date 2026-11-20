plugins {
    alias(libs.plugins.talkdraft.compose.multiplatform)
    alias(libs.plugins.talkdraft.koin.config)
}

kotlin {

    android {
        namespace = "com.sam.talkdraft.designsystem"

        androidResources {
            enable = true
        }
    }


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
            // koin
            implementation(libs.bundles.koin.common)
            implementation(libs.bundles.koin.compose)
            // local
            implementation(project(":core:common"))
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}


compose.resources {
    publicResClass = true
    packageOfResClass = "com.sam.talkdraft.designs"
    generateResClass = auto
    nameOfResClass = "CommonResources"

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
