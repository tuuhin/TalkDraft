plugins {
    alias(libs.plugins.talkdraft.android.library)
    alias(libs.plugins.talkdraft.koin.config)
}

android {
    namespace = "com.sam.talkdraft.transcription_android"
    ndkVersion = libs.versions.android.ndk.get()

    defaultConfig {
        consumerProguardFiles("consumer-rules.keep")
        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++20", "-Wno-format")
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

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    // testing
    androidTestImplementation(libs.koin.test)
    androidTestImplementation(libs.koin.test.junit)
    androidTestImplementation(libs.kotlin.test)
    androidTestImplementation(libs.kotlin.testJunit)
    androidTestImplementation(libs.asserrtk)
    androidTestImplementation(project(":core:testing"))
}
