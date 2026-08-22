plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.koin.compiler)
}

android {
    namespace = "com.sam.talkdraft.notifications"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        testInstrumentationRunner = "com.sam.talkdraft.testing.InstrumentTestRunner"

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.bundles.koin.common)
    androidTestImplementation(project(":core:testing"))
}
