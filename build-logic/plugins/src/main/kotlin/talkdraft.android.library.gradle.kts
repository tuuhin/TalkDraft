plugins {
    id("com.android.library")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

android {
    compileSdk {
        val majorVersion = libs.findVersion("android-compilesdk-major").get().requiredVersion.toInt()
        version = release(majorVersion) {
            minorApiLevel = libs.findVersion("android-compilesdk-revision").get().requiredVersion.toInt()
        }
    }

    defaultConfig {
        minSdk = libs.findVersion("android-minSdk").get().requiredVersion.toInt()
        testInstrumentationRunner = "com.sam.talkdraft.testing.InstrumentTestRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_22
        targetCompatibility = JavaVersion.VERSION_22
    }
}

