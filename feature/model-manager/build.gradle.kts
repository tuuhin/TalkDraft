plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.androidLint)
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {

    jvmToolchain(25)

    applyDefaultHierarchyTemplate()

    android {
        namespace = "com.sam.talkdraft.model_manager"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    listOf(iosArm64(), iosSimulatorArm64())

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.androidx.work.multiprocess)
        }

        commonMain.dependencies {
            // koin
            implementation(libs.koin.core)
            implementation(libs.koin.annotations)
            implementation(libs.bundles.koin.common)
            // supabase
            implementation(libs.supabase.postgrest)
            // okio
            implementation(libs.okio)
            // local
            implementation(project(":core:database"))
            implementation(project(":core:common"))
            implementation(project(":core:supabase"))
        }

    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
