plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {

    jvmToolchain(25)

    applyDefaultHierarchyTemplate()

    android {
        compileSdk {
            val majorVersion = libs.findVersion("android-compilesdk-major").get().requiredVersion.toInt()
            version = release(majorVersion) {
                minorApiLevel = libs.findVersion("android-compilesdk-revision").get().requiredVersion.toInt()
            }
        }
        minSdk = libs.findVersion("android-minSdk").get().requiredVersion.toInt()

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "com.sam.talkdraft.testing.InstrumentTestRunner"
            execution = "HOST"
        }

        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    iosArm64()
    iosSimulatorArm64()
}
