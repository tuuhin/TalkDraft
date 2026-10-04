plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.mokkery)
}

kotlin {

    jvmToolchain(25)

    applyDefaultHierarchyTemplate()

    android {
        namespace = "com.sam.talkdraft.feature_recordings"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {}.configure {}
    }

    listOf(iosArm64(), iosSimulatorArm64())

    sourceSets {
        commonMain.dependencies {
            implementation(libs.bundles.kotlinx.common)
            implementation(libs.bundles.koin.common)
            implementation(libs.okio)
            // local
            implementation(project(":core:common"))
            implementation(project(":core:database"))
            implementation(project(":core:player"))
            // feature
            implementation(project(":feature:model-manager"))
        }
        commonTest.dependencies {
            implementation(project(":core:testing"))
            implementation(libs.bundles.testing.common)
            implementation(libs.turbine)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
