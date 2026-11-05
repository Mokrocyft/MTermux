@file:Suppress("UnstableApiUsage", "DEPRECATION")

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.mtermux"
    compileSdk = libs.versions.compileSdk.get().toInt()
    ndkVersion = System.getenv("JITPACK_NDK_VERSION") ?: libs.versions.ndk.get()

    defaultConfig {
        applicationId = "com.mtermux"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.appVersionCode.get().toInt()
        versionName = libs.versions.appVersionName.get()

        ndk {
            abiFilters += setOf("arm64-v8a")
        }

        externalNativeBuild {
            cmake {
                arguments += listOf(
                    "-DANDROID_STL=c++_static"
                )
            }
        }
    }

    signingConfigs {
        create("releaseSigned") {
            storeFile = file("testkey_untrusted.jks")
            keyAlias = "alias"
            storePassword = "xrj45yWGLbsO7W0v"
            keyPassword = "xrj45yWGLbsO7W0v"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("releaseSigned")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    lint {
        disable += setOf("ProtectedPermissions", "ExpiredTargetSdkVersion")
        checkReleaseBuilds = false
        abortOnError = false
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += setOf(
                "META-INF/*.kotlin_module",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "DebugProbesKt.bin"
            )
        }
    }

    buildFeatures {
        buildConfig = false
    }
}

// Disable debug build variant completely so Gradle only configures and builds Release
androidComponents {
    beforeVariants(selector().withBuildType("debug")) { variantBuilder ->
        variantBuilder.enable = false
    }
}

dependencies {
    // Zero external Java libraries — 100% pure Kotlin 2.5.0-Beta2 + Android 17 framework + C++26 native core!
}

val prepareBootstrapAndFont = tasks.register<Exec>("prepareBootstrapAndFont") {
    description = "Ensures pre-patched bootstrap-aarch64.zip (for com.mtermux with OpenSSH) and JetBrainsMono.ttf are ready"
    workingDir = rootProject.projectDir
    commandLine("python3", "scripts/update_bootstrap.py")
    onlyIf {
        val bs = layout.projectDirectory.file("src/main/cpp/bootstrap-aarch64.zip").asFile
        val stamp = layout.projectDirectory.file("src/main/cpp/.bootstrap_patched_mtermux_v5.stamp").asFile
        val font = layout.projectDirectory.file("src/main/assets/fonts/JetBrainsMono.ttf").asFile
        !bs.exists() || bs.length() < 38_000_000L || !stamp.exists() || !font.exists() || font.length() < 50_000L
    }
}

tasks.named<Delete>("clean") {
    delete(fileTree("src/main/cpp") { include("bootstrap-*.zip", ".bootstrap_patched*.stamp") })
    delete(fileTree("src/main/assets/fonts") { include("*.ttf") })
}

tasks.named("preBuild") {
    dependsOn(prepareBootstrapAndFont)
}
