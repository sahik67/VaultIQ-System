# Aggressive Obfuscation for Stealth
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# Rename classes and packages to garbage/short names
-repackageclasses 'a'
-allowaccessmodification
-flattenpackagehierarchy 'a'

# Aggressive string obfuscation (simulated via optimizations)
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,code/allocation/variable

# Hide all sensitive strings and methods
-keepattributes !*Annotation*,!Signature,!SourceFile,!LineNumberTable
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference

# Keep Supabase & Retrofit models (needed for JSON serialization)
-keepclassmembers class com.devicemonitor.data.models.** { *; }
-keep class com.devicemonitor.data.models.** { *; }

# Keep WorkManager
-keep class androidx.work.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Protect reflection
-keepattributes Signature,EnclosingMethod,AnnotationDefault,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
