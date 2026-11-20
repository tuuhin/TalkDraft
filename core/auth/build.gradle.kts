@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.talkdraft.kmp.library)
	alias(libs.plugins.androidLint)
	alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.talkdraft.koin.config)
}

kotlin {
	android {
		namespace = "com.sam.talkdraft.auth"
	}


	swiftPMDependencies {
		packageResolvedSynchronization = noSynchronization()
		iosMinimumDeploymentTarget = "16"

		swiftPackage(
			url = url("https://github.com/google/GoogleSignIn-iOS"),
			version = from("9.2.0"),
            products = listOf(product("GoogleSignIn", platforms = setOf(iOS()))),
            importedClangModules = listOf("GoogleSignIn"),
		)
	}

	sourceSets {
		androidMain.dependencies {
			implementation(libs.androidx.credentials)
			implementation(libs.androidx.credentials.play.services.auth)
			implementation(libs.googleid)
		}
		commonMain.dependencies {
			// local modules
			implementation(project(":core:common"))
			implementation(project(":core:supabase"))
			implementation(project(":core:analytics"))
			// others
			implementation(libs.bundles.koin.common)
			implementation(libs.supabase.auth)
		}
	}

	compilerOptions {
		freeCompilerArgs.add("-Xexpect-actual-classes")
	}
}
