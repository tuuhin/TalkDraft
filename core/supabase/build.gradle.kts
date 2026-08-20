import com.codingfeline.buildkonfig.compiler.FieldSpec
import java.util.Properties

plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.androidMultiplatformLibrary)
	alias(libs.plugins.androidLint)
	alias(libs.plugins.build.konfig)
	alias(libs.plugins.koin.compiler)
	alias(libs.plugins.kotlinx.serialization)
}

kotlin {

	jvmToolchain(25)

	android {
		namespace = "com.sam.talkdraft.supabase"
		compileSdk = libs.versions.android.compileSdk.get().toInt()
		minSdk = libs.versions.android.minSdk.get().toInt()
	}

	listOf(iosArm64(), iosSimulatorArm64())

	sourceSets {
		commonMain.dependencies {
			// koin
			implementation(libs.koin.core)
			api(libs.koin.annotations)
			implementation("org.kotlincrypto.random:crypto-rand:0.6.0")

			implementation(libs.bundles.koin.common)
			implementation(libs.supabase.auth)
		}
	}

	compilerOptions {
		freeCompilerArgs.add("-Xexpect-actual-classes")
	}
}

buildkonfig {
	packageName = "com.sam.talkdraft.supabase"

	val commonProperties = Properties().apply {
		val commons = rootProject.file("secrets.properties")
		commons.inputStream().use(::load)
	}

	defaultConfigs {
		buildConfigField(
			type = FieldSpec.Type.STRING,
			name = "SUPABASE_API_URL",
			value = System.getenv("SUPABASE_API_URL")
				?: commonProperties.getProperty("SUPABASE_API_URL")
		)
		buildConfigField(
			type = FieldSpec.Type.STRING,
			name = "SUPABASE_API_KEY",
			value = System.getenv("SUPABASE_API_KEY")
				?: commonProperties.getProperty("SUPABASE_API_KEY")
		)

	}
}