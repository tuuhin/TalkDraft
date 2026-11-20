plugins {
    alias(libs.plugins.talkdraft.android.library)
    alias(libs.plugins.talkdraft.koin.config)
}

android {
    namespace = "com.sam.talkdraft.workers"
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
    implementation(project(":core:common"))
    implementation(project(":core:notification-android"))

    androidTestImplementation(libs.bundles.testing.android)
    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.work.test)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.mockk.agent)
}
