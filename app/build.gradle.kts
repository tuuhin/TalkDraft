plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.androidMultiplatformLibrary)
	alias(libs.plugins.androidLint)
	alias(libs.plugins.koin.compiler)
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
		}
	}

	sourceSets {
		commonMain.dependencies {

			api(project(":core:common"))
			api(project(":core:analytics"))
			api(project(":core:crashlytics"))

			implementation(libs.bundles.koin.common)
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

koinCompiler {
	userLogs = true
}