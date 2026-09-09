plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.androidLint)
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.skie)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.build.konfig)
}

kotlin {
    android {
        namespace = "com.sam.talkdraft.app"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
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
            linkerOpts.add("-lsqlite3")
            // export them to read the model classes
            export(project(":core:transcription-ios"))
            export(project(":core:background-jobs"))
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

            // feature modules
            implementation(project(":feature:model-manager"))
            implementation(project(":feature:model-downloader"))
            implementation(project(":feature:recorder"))
            implementation(project(":feature:onboarding"))

            // presentation module
            implementation(project(":presentation:design-system"))
            implementation(project(":presentation:navigation"))
            implementation(project(":presentation:onboarding"))
            implementation(project(":presentation:home"))
            implementation(project(":presentation:model-management"))
            implementation(project(":presentation:recorder"))
        }

        iosMain.dependencies {
            implementation(libs.kermit.koin)
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

composeCompiler {
    metricsDestination = layout.buildDirectory.dir("compose_compiler")
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("stability_config.conf"))
}

koinCompiler {
    userLogs = true
    strictSafety = true
    debugLogs = true
}

skie {
    build {
        enableSwiftLibraryEvolution.set(true)
    }
    features {
        enableSwiftUIObservingPreview = true
    }
}
