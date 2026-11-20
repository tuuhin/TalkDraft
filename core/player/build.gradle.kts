plugins {
    alias(libs.plugins.talkdraft.kmp.library)
    alias(libs.plugins.talkdraft.koin.config)
}

kotlin {

    android {
        namespace = "com.sam.talkdraft.player"
        androidResources {
            enable = true
        }
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.compilations.getByName("main").cinterops.create("nsExtras") {
            definitionFile = project.file("src/nativeInterop/cinterop/NSkeyObserver.def")
            packageName = "com.sam.talkdraft.platform.kvo"
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.exoplayer.dash)
            implementation(libs.androidx.media3.inspector)
            implementation(libs.androidx.concurrent)
        }
        commonMain.dependencies {
            implementation(libs.bundles.koin.common)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.kotlinx.datetime)
            // local
            implementation(project(":core:datastore"))
            implementation(project(":core:common"))
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.bundles.testing.android)
            implementation(libs.androidx.media3.testing)
            implementation(project(":core:testing"))
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
