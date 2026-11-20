import com.codingfeline.buildkonfig.compiler.FieldSpec
import java.util.Properties

plugins {
    alias(libs.plugins.talkdraft.kmp.library)
	alias(libs.plugins.build.konfig)
    alias(libs.plugins.talkdraft.koin.config)
	alias(libs.plugins.kotlinx.serialization)
}

kotlin {
	android {
		namespace = "com.sam.talkdraft.supabase"
	}


	sourceSets {
		commonMain.dependencies {
			// koin
			implementation(libs.koin.core)
			api(libs.koin.annotations)
            implementation(libs.crypto.rand)

			implementation(libs.bundles.koin.common)
			implementation(libs.supabase.auth)
            implementation(libs.supabase.postgrest)
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
