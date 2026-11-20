plugins {
    alias(libs.plugins.talkdraft.android.library)
    alias(libs.plugins.talkdraft.koin.config)
}

android {
    namespace = "com.sam.talkdraft.file_archive_android"
    ndkVersion = libs.versions.android.ndk.get()

    defaultConfig {
        consumerProguardFiles("consumer-rules.keep")

        externalNativeBuild {
            cmake {
                cFlags("-w")
                cppFlags += listOf("-std=c++20")
            }
        }
    }

    buildTypes {
        release {
            ndk {
                //noinspection ChromeOsAbiSupport
                abiFilters += listOf("armeabi-v7a", "arm64-v8a")
            }
        }
        debug {
            ndk {
                abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86_64")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    externalNativeBuild {
        cmake {
            version = "3.22.1"
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    packaging {
        jniLibs {
            pickFirsts.add("**/libonnxruntime.so")
            excludes += listOf("**/libparakeet.so")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.okio)
    // testing
    androidTestImplementation(libs.koin.test)
    androidTestImplementation(libs.koin.test.junit)
    androidTestImplementation(libs.kotlin.test)
    androidTestImplementation(libs.kotlin.testJunit)
    androidTestImplementation(libs.asserrtk)
    androidTestImplementation(project(":core:testing"))
}
