@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.talkdraft.kmp.library)
	alias(libs.plugins.androidLint)
    alias(libs.plugins.talkdraft.koin.config)
}

kotlin {
    android {
		namespace = "com.sam.talkdraft.crashlytics"
	}


	swiftPMDependencies {
		packageResolvedSynchronization = noSynchronization()
		swiftPackage(
            url = url("https://github.com/measure-sh/measure.git"),
            version = revision("ios-v0.17.0"),
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
