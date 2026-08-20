import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

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

		val properties = Properties().apply {
			val commons = rootProject.file("secrets.properties")
			commons.inputStream().use(::load)
		}

		manifestPlaceholders["MEASURE_API_KEY"] = properties.getProperty("MEASURE_ANDROID_KEY")
		manifestPlaceholders["MEASURE_API_URL"] = properties.getProperty("MEASURE_API_URL")

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
		buildConfig = true
	}
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_11
	}
}

dependencies {
	implementation(libs.androidx.activity.compose)
	implementation(libs.androidx.splash)

	// koin
	implementation(libs.bundles.koin.android)
	implementation(libs.bundles.koin.common)

	// local modules
	implementation(project(":app"))
}