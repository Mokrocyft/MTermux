-keep class com.mtermux.app.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
-optimizationpasses 5
-allowaccessmodification
-repackageclasses 'com.mtermux.internal'
-dontwarn android.**
