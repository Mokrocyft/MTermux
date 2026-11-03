import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI

plugins {
    alias(libs.plugins.android.application)
}

val fallbackBootstrapVersion: String = providers.gradleProperty("termux.bootstrapVersion")
    .getOrElse("2026.02.12-r1%2Bapt.android-7")

android {
    namespace = "com.termuxx"
    compileSdk = libs.versions.compileSdk.get().toInt()
    ndkVersion = System.getenv("JITPACK_NDK_VERSION") ?: libs.versions.ndk.get()

    defaultConfig {
        applicationId = "com.termuxx"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.appVersionCode.get().toInt()
        versionName = libs.versions.appVersionName.get()

        ndk {
            abiFilters += setOf("arm64-v8a")
        }

        externalNativeBuild {
            cmake {
                arguments += listOf("-DANDROID_STL=c++_static")
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
    // Zero external Java libraries — 100% pure Kotlin + Android framework + C++23 native core!
}

abstract class DownloadLatestBootstrapTask : DefaultTask() {
    @get:Input
    abstract val fallbackVersion: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun download() {
        val file = outputFile.get().asFile
        if (file.exists() && file.length() > 10_000_000L) return
        file.parentFile.mkdirs()

        val urls = listOf(
            "https://github.com/termux/termux-packages/releases/latest/download/bootstrap-aarch64.zip",
            "https://github.com/termux/termux-packages/releases/download/bootstrap-${fallbackVersion.get()}/bootstrap-aarch64.zip"
        )

        var downloaded = false
        for (url in urls) {
            try {
                logger.quiet("Downloading latest aarch64 bootstrap from $url ...")
                val conn = (URI.create(url).toURL().openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 30000
                    readTimeout = 60000
                }
                if (conn.responseCode in 200..299) {
                    conn.inputStream.use { input ->
                        BufferedOutputStream(FileOutputStream(file)).use { output ->
                            input.copyTo(output, bufferSize = 65536)
                        }
                    }
                    if (file.length() > 10_000_000L) {
                        logger.quiet("Downloaded bootstrap-aarch64.zip (${file.length()} bytes)")
                        downloaded = true
                        break
                    }
                }
            } catch (e: Exception) {
                logger.warn("Bootstrap download attempt from $url failed: ${e.message}")
            }
        }

        if (!downloaded || !file.exists() || file.length() <= 10_000_000L) {
            file.delete()
            throw GradleException("Failed to download aarch64 bootstrap archive")
        }
    }
}

abstract class DownloadFontTask : DefaultTask() {
    @get:Input
    abstract val fontUrl: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun download() {
        val file = outputFile.get().asFile
        if (file.exists() && file.length() > 50_000L) return
        file.parentFile.mkdirs()
        logger.quiet("Downloading JetBrainsMono[wght].ttf from ${fontUrl.get()} ...")
        val connection = (URI.create(fontUrl.get()).toURL().openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 30000
            readTimeout = 60000
        }
        connection.inputStream.use { input ->
            BufferedOutputStream(FileOutputStream(file)).use { output ->
                input.copyTo(output, bufferSize = 32768)
            }
        }
        if (file.length() < 50_000L) {
            file.delete()
            throw GradleException("Downloaded JetBrainsMono.ttf is unexpectedly small")
        }
    }
}

val downloadBootstraps = tasks.register<DownloadLatestBootstrapTask>("downloadBootstraps") {
    description = "Downloads the latest official Termux aarch64 bootstrap archive"
    fallbackVersion.set(fallbackBootstrapVersion)
    outputFile.set(layout.projectDirectory.file("src/main/cpp/bootstrap-aarch64.zip"))
}

val downloadJetBrainsMono = tasks.register<DownloadFontTask>("downloadJetBrainsMono") {
    description = "Downloads JetBrainsMono[wght].ttf into assets/fonts/JetBrainsMono.ttf"
    fontUrl.set("https://raw.githubusercontent.com/google/fonts/main/ofl/jetbrainsmono/JetBrainsMono%5Bwght%5D.ttf")
    outputFile.set(layout.projectDirectory.file("src/main/assets/fonts/JetBrainsMono.ttf"))
}

tasks.named<Delete>("clean") {
    delete(fileTree("src/main/cpp") { include("bootstrap-*.zip", ".bootstrap_patched.stamp") })
    delete(fileTree("src/main/assets/fonts") { include("*.ttf") })
}

tasks.named("preBuild") {
    dependsOn(downloadBootstraps, downloadJetBrainsMono)
}
