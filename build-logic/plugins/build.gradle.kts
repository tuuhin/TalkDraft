plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.android.gradle.plugin)
    implementation(libs.compose.gradle.plugin)
    implementation(libs.kotlin.compose.gradle.plugin)
    implementation(libs.koin.gradle.plugin)
}
