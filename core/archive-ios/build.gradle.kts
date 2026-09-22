@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}


kotlin {

    jvmToolchain(25)

    applyDefaultHierarchyTemplate()

    // only ios targets
    listOf(iosArm64(), iosSimulatorArm64())

    swiftPMDependencies {
        packageResolvedSynchronization = noSynchronization()

        localSwiftPackage(
            directory = layout.projectDirectory.dir("src/native"),
            products = listOf(product("IosNativeTar")),
        )
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kermit)
            implementation(libs.okio)
        }
    }
}
