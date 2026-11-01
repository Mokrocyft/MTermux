import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URI
import java.security.DigestInputStream
import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.application)
}

val termuxBootstrapVersion: String = providers.gradleProperty("termux.bootstrapVersion")
    .getOrElse("2026.02.12-r1%2Bapt.android-7")
val termuxBootstrapSha256Aarch64: String = providers.gradleProperty("termux.bootstrapSha256Aarch64")
    .getOrElse("ea2aeba8819e517db711f8c32369e89e7c52cee73e07930ff91185e1ab93f4f3")

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
    }

    // Embedded signing keystore so both Debug and Release APKs are pre-signed out of the box
    signingConfigs {
        named("debug") {
            storeFile = file("testkey_untrusted.jks")
            keyAlias = "alias"
            storePassword = "xrj45yWGLbsO7W0v"
            keyPassword = "xrj45yWGLbsO7W0v"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
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
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = false
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
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.annotation)
}

abstract class DownloadBootstrapTask : DefaultTask() {
    @get:Input
    abstract val arch: Property<String>

    @get:Input
    abstract val expectedChecksum: Property<String>

    @get:Input
    abstract val bootstrapVersion: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun download() {
        val file = outputFile.get().asFile
        val expected = expectedChecksum.get()
        val digest = MessageDigest.getInstance("SHA-256")

        if (file.exists()) {
            FileInputStream(file).use { input ->
                val buffer = ByteArray(16384)
                var read: Int
                while (input.read(buffer).also { read = it } >= 0) {
                    digest.update(buffer, 0, read)
                }
            }
            val actual = BigInteger(1, digest.digest()).toString(16).padStart(64, '0')
            if (actual == expected) return
            file.delete()
        }

        val remoteUrl = "https://github.com/termux/termux-packages/releases/download/bootstrap-${bootstrapVersion.get()}/bootstrap-${arch.get()}.zip"
        logger.quiet("Downloading aarch64 bootstrap from $remoteUrl ...")
        file.parentFile.mkdirs()

        val digest2 = MessageDigest.getInstance("SHA-256")
        val connection = (URI.create(remoteUrl).toURL().openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 30000
            readTimeout = 60000
        }
        DigestInputStream(connection.inputStream, digest2).use { input ->
            BufferedOutputStream(FileOutputStream(file)).use { output ->
                input.copyTo(output, bufferSize = 16384)
            }
        }

        val actual = BigInteger(1, digest2.digest()).toString(16).padStart(64, '0')
        if (actual != expected) {
            file.delete()
            throw GradleException("Checksum mismatch for $remoteUrl: expected $expected, actual $actual")
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
                input.copyTo(output, bufferSize = 16384)
            }
        }
        if (file.length() < 50_000L) {
            file.delete()
            throw GradleException("Downloaded JetBrainsMono.ttf is unexpectedly small")
        }
    }
}

val downloadBootstraps = tasks.register<DownloadBootstrapTask>("downloadBootstraps") {
    description = "Downloads the official Termux aarch64 bootstrap archive for apt/pkg"
    arch.set("aarch64")
    expectedChecksum.set(termuxBootstrapSha256Aarch64)
    bootstrapVersion.set(termuxBootstrapVersion)
    outputFile.set(layout.projectDirectory.file("src/main/cpp/bootstrap-aarch64.zip"))
}

val downloadJetBrainsMono = tasks.register<DownloadFontTask>("downloadJetBrainsMono") {
    description = "Downloads JetBrainsMono[wght].ttf into assets/fonts/JetBrainsMono.ttf"
    fontUrl.set("https://raw.githubusercontent.com/google/fonts/main/ofl/jetbrainsmono/JetBrainsMono%5Bwght%5D.ttf")
    outputFile.set(layout.projectDirectory.file("src/main/assets/fonts/JetBrainsMono.ttf"))
}

tasks.named<Delete>("clean") {
    delete(fileTree("src/main/cpp") { include("bootstrap-*.zip") })
    delete(fileTree("src/main/assets/fonts") { include("*.ttf") })
}

tasks.named("preBuild") {
    dependsOn(downloadBootstraps, downloadJetBrainsMono)
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn(downloadBootstraps, downloadJetBrainsMono)
    options.compilerArgs.addAll(listOf("-Xlint:-deprecation", "-Xlint:-unchecked", "-Xlint:-rawtypes"))
}

tasks.configureEach {
    if (name.startsWith("configureCMake") || name.startsWith("buildCMake") || name.startsWith("externalNativeBuild") || name.startsWith("merge")) {
        dependsOn(downloadBootstraps, downloadJetBrainsMono)
    }
}
