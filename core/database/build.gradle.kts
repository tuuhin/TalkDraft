plugins {
    alias(libs.plugins.talkdraft.kmp.library)
    alias(libs.plugins.talkdraft.koin.config)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room3)
    alias(libs.plugins.atomic.fu)
}

kotlin {
    jvmToolchain(25)

    android {
        namespace = "com.sam.talkdraft.database"
    }


    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.room3.sqlite.wrapper)
        }
        commonMain.dependencies {
            // local modules
            implementation(project(":core:common"))
            // others
            implementation(libs.bundles.koin.common)
            implementation(libs.okio)
            // room
            implementation(libs.androidx.room3.runtime)
            implementation(libs.androidx.sqlite.framework)
            // datatime
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.koin.test)
            implementation(project(":core:testing"))
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

atomicfu {
    transformJvm = false
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.androidx.room3.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room3.compiler)
    add("kspIosArm64", libs.androidx.room3.compiler)
}
