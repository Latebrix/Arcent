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
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-repackageclasses 'tech.arcent'
-ignorewarnings
-keepattributes Signature
-keepattributes *Annotation*
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

###################################################################################################
# Custom rules added for project dependencies (see app/build.gradle.kts)
# NOTE: Currently release build has isMinifyEnabled = false. These rules take effect once you set
#       isMinifyEnabled = true (recommended for production to shrink/obfuscate). Enable when ready.
###################################################################################################

########## Kotlin / General ##########
# Keep Kotlin metadata (usually kept automatically, but explicit for safety with reflection libs)
-keep class kotlin.Metadata { *; }

########## Room (reflection on annotated entities / DAO interfaces) ##########
# Keep @Entity annotated classes and their fields & methods
-keep @androidx.room.Entity class * { *; }
# Keep DAO interfaces
-keep interface * extends androidx.room.Dao
-keep @androidx.room.Dao interface * { *; }
# Keep RoomDatabase implementations
-keep class * extends androidx.room.RoomDatabase { *; }

########## Dagger / Hilt ##########
# Hilt ships consumer ProGuard rules; the below are defensive additions.
-dontwarn dagger.hilt.internal.**
-keep class dagger.hilt.internal.generated.** { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }

########## Sentry (already provides consumer rules; keep full classes to preserve stack traces) ##########
-keep class io.sentry.** { *; }
-dontwarn io.sentry.**

########## Appwrite SDK ##########
-keep class io.appwrite.** { *; }
-dontwarn io.appwrite.**

########## AndroidX Security Crypto / Tink ##########
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

########## OkHttp / Okio (avoid warnings for shaded or optional classes) ##########
-dontwarn okhttp3.internal.platform.**
-dontwarn okio.**

########## Coroutines (normally safe; silence potential metadata warnings) ##########
-dontwarn kotlinx.coroutines.**

########## Coil (has consumer rules; no additional keep needed, but silence warnings just in case) ##########
-dontwarn coil.**

########## Google Play Services Auth ##########
-dontwarn com.google.android.gms.auth.**

########## Gson (if indirectly used via Appwrite; keep generic signatures for TypeToken) ##########
-keepattributes Signature

# End of custom rules.
