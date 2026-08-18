import com.codingfeline.buildkonfig.compiler.FieldSpec
import java.util.Properties

plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.androidMultiplatformLibrary)
	alias(libs.plugins.androidLint)
	alias(libs.plugins.build.konfig)
}

kotlin {
	android {
		namespace = "com.sam.talkdraft.commons"
		compileSdk = libs.versions.android.compileSdk.get().toInt()
		minSdk = libs.versions.android.minSdk.get().toInt()
	}

	listOf(iosArm64(), iosSimulatorArm64())

	sourceSets {
		commonMain.dependencies {
			api(libs.kermit)
		}
	}
}

buildkonfig {
	packageName = "com.sam.talkdraft.commons"
	exposeObjectWithName = "AppSecretProperties"

	val commonProperties = Properties().apply {
		val commons = rootProject.file("secrets.properties")
		commons.inputStream().use(::load)
	}

	defaultConfigs {
		buildConfigField(
			type = FieldSpec.Type.STRING,
			name = "POST_HOG_API_KEY",
			value = System.getenv("POST_HOG_API_KEY")
				?: commonProperties.getProperty("POST_HOG_API_KEY")
		)
		buildConfigField(
			type = FieldSpec.Type.STRING,
			name = "MEASURE_ANDROID_API_KEY",
			value = System.getenv("MEASURE_ANDROID_KEY")
				?: commonProperties.getProperty("MEASURE_ANDROID_KEY")
		)

		buildConfigField(
			type = FieldSpec.Type.STRING,
			name = "MEASURE_IOS_KEY",
			value = System.getenv("MEASURE_IOS_KEY")
				?: commonProperties.getProperty("MEASURE_IOS_KEY")
		)
	}
}