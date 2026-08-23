plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.koin.compiler)
}

android {
    namespace = "com.sam.talkdraft.workers"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        testInstrumentationRunner = "com.sam.talkdraft.testing.InstrumentTestRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_22
        targetCompatibility = JavaVersion.VERSION_22
    }
}

dependencies {
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.work.multiprocess)

    implementation(libs.bundles.koin.common)
    implementation(libs.koin.androidx.workmanager)
    implementation(libs.kermit)

    implementation(project(":feature:model-manager"))
    implementation(project(":feature:model-downloader"))
    implementation(project(":core:analytics"))
    implementation(project(":core:notification-android"))
    androidTestImplementation(project(":core:testing"))
}
