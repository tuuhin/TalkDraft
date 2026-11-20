plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.androidLint)
    alias(libs.plugins.talkdraft.koin.config)
    alias(libs.plugins.skie)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.build.konfig)
}

kotlin {
    android {
        namespace = "com.sam.talkdraft.app"
        compileSdk {
            val majorVersion = libs.versions.android.compilesdk.major.get().toInt()
            val minorVersion = libs.versions.android.compilesdk.revision.get().toInt()
            version = release(majorVersion) {
                minorApiLevel = minorVersion
            }
        }
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    val xcfName = "TalkDraftApp"
    val iosTargets = listOf(iosArm64(), iosSimulatorArm64())
    iosTargets.forEach { target ->
        target.binaries.framework {
            baseName = xcfName
            // required for room
            linkerOpts.add("-lsqlite3")
            // bridge binding for worker
            export(project(":core:background-jobs"))
            export(libs.kermit.simple)
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        androidMain.dependencies {
            // android modules
            api(project(":core:notification-android"))
            implementation(project(":core:worker-android"))
        }
        commonMain.dependencies {
            implementation(libs.bundles.compose.ui)
            implementation(libs.bundles.koin.compose)
            // core modules
            api(project(":core:common"))
            api(project(":core:analytics"))
            api(project(":core:crashlytics"))
            api(project(":core:database"))
            api(project(":core:supabase"))
            api(project(":core:auth"))
            api(project(":core:platform-capability"))
            api(project(":core:connectivity"))
            api(project(":core:datastore"))
            api(project(":core:recorder"))
            api(project(":core:recorder-visualizer"))
            api(project(":core:player"))
            api(project(":core:transcription-core"))
            api(project(":core:background-jobs"))
            api(project(":core:remote-config"))
            api(project(":core:permissions"))
            api(project(":core:archive-core"))
            api(project(":core:payments"))
            // feature modules
            implementation(project(":feature:model-manager"))
            implementation(project(":feature:model-downloader"))
            implementation(project(":feature:recorder"))
            implementation(project(":feature:onboarding"))
            implementation(project(":feature:recordings"))
            // presentation module
            implementation(project(":presentation:design-system"))
            implementation(project(":presentation:navigation"))
            implementation(project(":presentation:onboarding"))
            implementation(project(":presentation:home"))
            implementation(project(":presentation:model-management"))
            implementation(project(":presentation:recorder"))
            implementation(project(":presentation:settings"))
            implementation(project(":presentation:recordings"))
        }

        iosMain.dependencies {
            implementation(libs.kermit.koin)
            api(libs.kermit.simple)
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.core)
                implementation(libs.androidx.runner)
                implementation(libs.androidx.testExt.junit)
            }
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.experimental.ExperimentalObjCRefinement")
    }
}


skie {
    build {
        enableSwiftLibraryEvolution = true
        enableParallelSwiftCompilation = true
    }
    swiftBundling {
        enabled = true
    }
}
