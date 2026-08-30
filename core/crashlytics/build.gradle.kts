@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.androidMultiplatformLibrary)
	alias(libs.plugins.androidLint)
	alias(libs.plugins.koin.compiler)
}

kotlin {

    jvmToolchain(25)

	android {
		namespace = "com.sam.talkdraft.crashlytics"
		compileSdk = libs.versions.android.compileSdk.get().toInt()
		minSdk = libs.versions.android.minSdk.get().toInt()
	}

	listOf(iosArm64(), iosSimulatorArm64())

	swiftPMDependencies {
		packageResolvedSynchronization = noSynchronization()
		swiftPackage(
            url = url("https://github.com/measure-sh/measure.git"),
            version = revision("ios-v0.13.0"),
            products = listOf(product("Measure")),
            packageName = "measure",
        )
	}

	sourceSets {
		androidMain.dependencies {
			implementation(libs.measure.android)
		}
		commonMain.dependencies {
			// local modules
			implementation(project(":core:common"))

			implementation(libs.bundles.koin.common)
		}
	}

	compilerOptions {
		freeCompilerArgs.add("-Xexpect-actual-classes")
	}
}
