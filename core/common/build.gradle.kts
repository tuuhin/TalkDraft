import com.codingfeline.buildkonfig.compiler.FieldSpec
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.androidLint)
    alias(libs.plugins.build.konfig)
    alias(libs.plugins.koin.compiler)
}

kotlin {

    jvmToolchain(25)

    android {
        namespace = "com.sam.talkdraft.commons"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTest {
            isIncludeAndroidResources = true
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "com.sam.talkdraft.testing.InstrumentTestRunner"
            execution = "HOST"
        }
    }

    listOf(iosArm64(), iosSimulatorArm64())

    applyDefaultHierarchyTemplate()

    sourceSets {
        androidMain.dependencies {
            implementation(ktorLibs.client.okhttp)
        }
        commonMain.dependencies {
            api(libs.kermit)
            // io
            implementation(libs.okio)
            // koin
            implementation(libs.bundles.koin.common)
            // crypto
            implementation(libs.crypto.rand)
            // coroutines
            implementation(libs.kotlinx.coroutines.core)
            // ktor client
            implementation(ktorLibs.client.core)
            implementation(ktorLibs.client.contentNegotiation)
            implementation(ktorLibs.serialization.kotlinx.json)
            implementation(ktorLibs.client.logging)
        }
        commonTest.dependencies {
            implementation(project(":core:testing"))
        }
        iosMain.dependencies {
            implementation(ktorLibs.client.darwin)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
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
                ?: commonProperties.getProperty("POST_HOG_API_KEY"),
        )

        buildConfigField(
            type = FieldSpec.Type.STRING,
            name = "GOOGLE_SIGN_IN_WEB_CLIENT_ID",
            value = System.getenv("GOOGLE_SIGN_IN_WEB_CLIENT_ID")
                ?: commonProperties.getProperty("GOOGLE_SIGN_IN_WEB_CLIENT_ID"),
        )
    }

    targetConfigs {

        create("android") {
            buildConfigField(
                type = FieldSpec.Type.STRING,
                name = "MEASURE_ANDROID_API_KEY",
                value = System.getenv("MEASURE_ANDROID_KEY")
                    ?: commonProperties.getProperty("MEASURE_ANDROID_KEY"),
            )
        }

        create("ios") {

            buildConfigField(
                type = FieldSpec.Type.STRING,
                name = "MEASURE_IOS_KEY",
                value = System.getenv("MEASURE_IOS_KEY")
                    ?: commonProperties.getProperty("MEASURE_IOS_KEY"),
            )

            buildConfigField(
                type = FieldSpec.Type.STRING,
                name = "GOOGLE_IOS_SIGN_IN_CLIENT_ID",
                value = System.getenv("GOOGLE_IOS_SIGN_IN_CLIENT_ID")
                    ?: commonProperties.getProperty("GOOGLE_IOS_SIGN_IN_CLIENT_ID"),
            )
        }
    }
}
