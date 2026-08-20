plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.androidMultiplatformLibrary)
	alias(libs.plugins.androidLint)
	alias(libs.plugins.koin.compiler)
	alias(libs.plugins.ksp)
	alias(libs.plugins.androidx.room3)
	alias(libs.plugins.atomic.fu)
}

kotlin {
	jvmToolchain(25)

	android {
		namespace = "com.sam.talkdraft.database"
		compileSdk = libs.versions.android.compileSdk.get().toInt()
		minSdk = libs.versions.android.minSdk.get().toInt()
	}

	listOf(iosArm64(), iosSimulatorArm64())

	sourceSets {
		androidMain.dependencies {
			implementation(libs.androidx.room3.sqlite.wrapper)
		}
		commonMain.dependencies {
			// local modules
			implementation(project(":core:common"))
			// others
			implementation(libs.bundles.koin.common)
			implementation(libs.okio)
			// room
			implementation(libs.androidx.room3.runtime)
			implementation(libs.androidx.sqlite.framework)
		}
		iosMain.dependencies {
		}
	}

	compilerOptions {
		freeCompilerArgs.add("-Xexpect-actual-classes")
	}
}

room3 {
	schemaDirectory("$projectDir/schemas")
}

dependencies {
	add("kspAndroid", libs.androidx.room3.compiler)
	add("kspIosSimulatorArm64", libs.androidx.room3.compiler)
	add("kspIosArm64", libs.androidx.room3.compiler)
}