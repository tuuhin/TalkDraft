plugins {
    alias(libs.plugins.talkdraft.kmp.library)
    alias(libs.plugins.androidLint)
    alias(libs.plugins.talkdraft.koin.config)
}

kotlin {
    android {
        namespace = "com.sam.talkdraft.datastore"
    }


    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
        }

        commonMain.dependencies {
            // koin
            implementation(libs.bundles.koin.common)
            // datastore
            implementation(libs.bundles.datastore)
            implementation(project(":core:common"))
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
