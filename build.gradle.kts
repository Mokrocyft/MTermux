buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.5.0-Beta2")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
}
