plugins {
    alias(libs.plugins.talkdraft.android.library)
    alias(libs.plugins.talkdraft.koin.config)
}

android {
    namespace = "com.sam.talkdraft.notifications"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.bundles.koin.common)
    androidTestImplementation(project(":core:testing"))
}
