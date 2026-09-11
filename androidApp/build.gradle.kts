import java.nio.file.FileSystems
import java.nio.file.Files
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.compose.compiler)
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

    signingConfigs {

        val keySecretFile = project.file("keystore.properties")
        if (!keySecretFile.exists()) return@signingConfigs

        val properties = Properties()
        keySecretFile.inputStream().use { properties.load(it) }

        val storeFileName = properties.getProperty("STORE_FILE")
            ?: return@signingConfigs

        val keyStoreFile = project.file(storeFileName)
        if (!keyStoreFile.exists()) return@signingConfigs

        create("release") {
            storeFile = keyStoreFile
            keyAlias = properties.getProperty("KEY_ALIAS")
            keyPassword = properties.getProperty("KEY_PASSWORD")
            storePassword = properties.getProperty("STORE_PASSWORD")
            enableV3Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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
    // android
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splash)

    // koin
    implementation(libs.bundles.koin.android)
    implementation(libs.bundles.koin.common)
    implementation(libs.koin.androidx.workmanager)

    // local modules
    implementation(project(":app"))
    implementation(project(":core:permissions"))
    implementation(project(":feature:onboarding"))
}

// Workaround for AGP 9.4.0+ JarFlinger validation: R8 synthesizes
// META-INF/MaterialKolor:material-kolor.kotlin_module into base.jar. Because the entry name contains
// a colon, JarFlinger throws InvalidPathException in buildReleasePreBundle.
tasks.matching { it.name == "minifyReleaseWithR8" }.configureEach {

    val jarFileProvider = project.layout.buildDirectory
        .file("intermediates/merged_java_res/release/minifyReleaseWithR8/base.jar")

    doLast {
        val jarFile = jarFileProvider.get().asFile
        if (jarFile.exists()) return@doLast

        FileSystems.newFileSystem(jarFile.toPath()).use { fs ->
            val invalidEntry = fs.getPath("META-INF/MaterialKolor:material-kolor.kotlin_module")
            if (Files.exists(invalidEntry)) {
                Files.delete(invalidEntry)
                logger.lifecycle("Removed invalid zip entry from ${jarFile.name}")
            }
        }
    }

}
