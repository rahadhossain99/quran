# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# OkHttp/Okio optional security dependencies
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.bouncycastle.crypto.tls.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# General ProGuard rules for common libraries
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.**

# Keep app package models, db classes, service and receivers intact
-keep class com.example.data.model.** { *; }
-keep class com.example.data.db.** { *; }
-keep class com.example.data.api.** { *; }
-keep class com.example.data.service.** { *; }
-keep class com.example.data.receiver.** { *; }
-keep class com.example.viewmodel.** { *; }

# Moshi rules
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**
# Keep Moshi generated adapters
-keep class *JsonAdapter { *; }
-keep class *CustomAdapterMethod { *; }

# Retrofit rules
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp3 rules
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Room rules
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase$Callback
-keep class * extends androidx.room.Entity
-keep class * extends androidx.room.Dao
-dontwarn androidx.room.**

# Kotlin Coroutines and Metadata
-keepclassmembers class * {
    *** Companion;
}
-keep class kotlin.Metadata { *; }
-dontwarn kotlinx.coroutines.**

