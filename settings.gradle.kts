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

include(":androidApp")
// modules
include(":core:common")
include(":core:analytics")
include(":core:crashlytics")
include(":core:auth")
include(":core:database")
include(":core:supabase")
include(":core:testing")

include(":app")
