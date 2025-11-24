# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Exclude BuildConfig from classes.jar to avoid duplicate class errors
# (AAR already contains BuildConfig)
-dontwarn com.wit.sdk.BuildConfig
-keep class !com.wit.sdk.BuildConfig,com.wit.** { *; }

