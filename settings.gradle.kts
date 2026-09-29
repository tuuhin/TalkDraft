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
            from("io.ktor:ktor-version-catalog:3.6.0")
        }
    }
}

// entry point
include(":androidApp")
include(":androidBenchMark")
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
include(":core:datastore")
include(":core:recorder")
include(":core:recorder-visualizer")
include(":core:player")
include(":core:background-jobs")
include(":core:transcription-core")
include(":core:transcription-android")
include(":core:transcription-ios")
include(":core:permissions")
include(":core:archive-core")
include(":core:archive-android")
include(":core:archive-ios")
// feature modules
include(":feature:model-manager")
include(":feature:model-downloader")
include(":feature:onboarding")
include(":feature:recorder")
// ui layers
include(":presentation:design-system")
include(":presentation:onboarding")
include(":presentation:navigation")
include(":presentation:home")
include(":presentation:model-management")
include(":presentation:recorder")
// app layer to hold platform
include(":app")
