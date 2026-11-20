plugins {
    alias(libs.plugins.talkdraft.kmp.library)
    alias(libs.plugins.talkdraft.koin.config)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    android {
        namespace = "com.sam.talkdraft.model_manager"
    }


    sourceSets {
        commonMain.dependencies {
            // koin
            implementation(libs.koin.core)
            implementation(libs.koin.annotations)
            implementation(libs.bundles.koin.common)
            // supabase
            implementation(libs.supabase.postgrest)
            implementation(libs.androidx.datastore.preferences)
            // okio
            implementation(libs.okio)
            // local
            implementation(project(":core:platform-capability"))
            implementation(project(":core:datastore"))
            implementation(project(":core:database"))
            implementation(project(":core:common"))
            implementation(project(":core:supabase"))
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
