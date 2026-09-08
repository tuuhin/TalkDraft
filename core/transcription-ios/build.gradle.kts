plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "com.sam.talkdraft.transcription.ios"

kotlin {

    jvmToolchain(25)

    applyDefaultHierarchyTemplate()

    // only ios targets
    listOf(iosArm64(), iosSimulatorArm64())

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kermit)
        }
    }
}
