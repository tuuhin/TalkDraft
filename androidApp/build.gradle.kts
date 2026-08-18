import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.androidApplication)
	alias(libs.plugins.composeCompiler)
	alias(libs.plugins.koin.compiler)
}

android {
	namespace = "com.sam.talkdraft"
	compileSdk = libs.versions.android.compileSdk.get().toInt()

	defaultConfig {
		applicationId = "com.sam.talkdraft"
		minSdk = libs.versions.android.minSdk.get().toInt()
		targetSdk = libs.versions.android.targetSdk.get().toInt()
		versionCode = 1
		versionName = "1.0"
	}
	packaging {
		resources {
			excludes += "/META-INF/{AL2.0,LGPL2.1}"
		}
	}
	buildTypes {
		release {
			isMinifyEnabled = true
			isShrinkResources = true
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_11
		targetCompatibility = JavaVersion.VERSION_11
	}
	buildFeatures {
		compose = true
	}
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_11
	}
}

koinCompiler {
	userLogs = true
}

dependencies {
	implementation(libs.androidx.activity.compose)
	implementation(libs.compose.uiToolingPreview)

	// koin
	implementation(libs.bundles.koin.android)
	implementation(libs.bundles.koin.common)

	// local modules
	implementation(project(":core:common"))
	implementation(project(":core:analytics"))

	debugImplementation(libs.compose.uiTooling)
}