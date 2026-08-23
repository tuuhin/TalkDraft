rootProject.name = "TalkDraft"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }

    versionCatalogs {
        create("ktorLibs") {
            from("io.ktor:ktor-version-catalog:3.5.2")
        }
    }
}

// entry point
include(":androidApp")
// modules
include(":core:common")
include(":core:analytics")
include(":core:crashlytics")
include(":core:auth")
include(":core:database")
include(":core:supabase")
include(":core:testing")
include(":core:platform-capability")
include(":core:connectivity")
include(":core:notification-android")
include(":core:worker-android")
include(":core:remote-config")
// feature modules
include(":feature:transcription-core")
include(":feature:transcription-android")
include(":feature:transcription-ios")
include(":feature:model-manager")
include(":feature:model-downloader")

include(":app")
