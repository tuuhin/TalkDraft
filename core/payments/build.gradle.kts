plugins {
    alias(libs.plugins.talkdraft.kmp.library)
    alias(libs.plugins.talkdraft.koin.config)
}

kotlin {
    android {
        namespace = "com.sam.talkdraft.payments"
        androidResources {
            enable = true
        }
    }


    sourceSets {

        commonMain.dependencies {
            implementation(libs.kermit)
            implementation(libs.kotlinx.collections.immutable)
            // koin
            implementation(libs.bundles.koin.common)
            // payments
            implementation(libs.purchases.core)
            implementation(libs.purchases.result)
            // local
            implementation(project(":core:common"))
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
