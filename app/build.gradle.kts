import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.androidMultiplatformLibrary)
	alias(libs.plugins.androidLint)
	alias(libs.plugins.koin.compiler)
	alias(libs.plugins.skie)
	alias(libs.plugins.composeCompiler)
	alias(libs.plugins.build.konfig)
}

kotlin {
	android {
		namespace = "com.sam.talkdraft.app"
		compileSdk = libs.versions.android.compileSdk.get().toInt()
		minSdk = libs.versions.android.minSdk.get().toInt()

		withHostTestBuilder {

		}
		withDeviceTestBuilder {
			sourceSetTreeName = "test"
		}.configure {
			instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		}
	}

	val xcfName = "TalkDraftApp"
	val iosTargets = listOf(iosArm64(), iosSimulatorArm64())
	iosTargets.forEach { target ->
		target.binaries.framework {
			baseName = xcfName
			linkerOpts.add("-lsqlite3")
		}
	}

	applyDefaultHierarchyTemplate()

	sourceSets {
		androidMain.dependencies {
			implementation(libs.bundles.compose.ui)
			implementation(libs.bundles.koin.compose)
		}
		commonMain.dependencies {

			api(project(":core:common"))
			api(project(":core:analytics"))
			api(project(":core:crashlytics"))
//			implementation(project(":core:database"))
			api(project(":core:supabase"))
			api(project(":core:auth"))
		}

		iosMain.dependencies {
			implementation(libs.kermit.koin)
		}

		getByName("androidDeviceTest") {
			dependencies {
				implementation(libs.androidx.core)
				implementation(libs.androidx.runner)
				implementation(libs.androidx.testExt.junit)
			}
		}
	}
}

configure<ComposeCompilerGradlePluginExtension> {
	targetKotlinPlatforms = setOf(KotlinPlatformType.androidJvm)
}

koinCompiler {
	userLogs = true
	strictSafety = true
}

buildkonfig {
	packageName = "com.sam.talkdraft.app"

	defaultConfigs {}

	targetConfigs {
		create("ios") {
			buildConfigField(
				type = FieldSpec.Type.BOOLEAN,
				name = "SETUP_POSTHOG_AND_MEASURE",
				value = "false"
			)
		}
	}
}

skie {
	build {
		enableSwiftLibraryEvolution.set(true)
	}
	features {
		enableSwiftUIObservingPreview = true
	}
}