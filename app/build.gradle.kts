import com.codingfeline.buildkonfig.compiler.FieldSpec

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

        withHostTestBuilder {

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
            export(project(":feature:transcription-ios"))
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        androidMain.dependencies {
            // android modules
            api(project(":core:notification-android"))
            api(project(":core:worker-android"))
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
            // feature modules
            api(project(":feature:transcription-core"))
            api(project(":feature:model-manager"))
            api(project(":feature:model-downloader"))
            api(project(":feature:onboarding"))
            api(project(":feature:recorder"))

            // presentation module
            implementation(project(":presentation:design-system"))
            implementation(project(":presentation:navigation"))
            implementation(project(":presentation:onboarding"))
            implementation(project(":presentation:home"))
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


koinCompiler {
    userLogs = true
    strictSafety = true
    debugLogs = true
}

buildkonfig {
    packageName = "com.sam.talkdraft.app"

    defaultConfigs {}

    targetConfigs {
        create("ios") {
            buildConfigField(
                type = FieldSpec.Type.BOOLEAN,
                name = "SETUP_POSTHOG_AND_MEASURE",
                value = "false",
            )
        }
    }
}

skie {
    build {
        enableSwiftLibraryEvolution.set(true)
    }
    features {
        enableSwiftUIObservingPreview = true
    }
}
