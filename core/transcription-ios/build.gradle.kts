plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "com.sam.talkdraft.transcription.ios"

kotlin {

    jvmToolchain(25)

    applyDefaultHierarchyTemplate()


    listOf(iosArm64(), iosSimulatorArm64())

    sourceSets {

        commonMain.dependencies {
            // koin
            implementation(libs.koin.core)
            api(libs.koin.annotations)
            implementation(libs.bundles.koin.common)
            implementation(libs.supabase.auth)
        }
    }

}
