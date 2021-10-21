# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/mhefny/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-dontobfuscate

-dontwarn com.squareup.**
-dontwarn okio.**
-dontwarn okhttp.**
-dontwarn org.codehaus.jackson.map.**
-dontwarn org.codehaus.jackson.node.**
-dontwarn org.codehaus.jackson.schema.**
-dontwarn com.androidplot.**
-dontwarn de.tavendo.autobahn.**
-dontwarn android.support.**



-keepattributes *Annotation*,EnclosingMethod,Signature,InnerClasses,Deprecated,Exceptions,EnclosingMethod

-keepclasseswithmembernames class * {
    native <methods>;
}


-keep class **       {  public *;  protected *;}
-keep interface **   {  public *;  protected *;}
-keep enum **        {  public *;  protected *;}



-keep class     android.support.** { *; }
-keep interface android.support.** { *; }
-keep class     com.google.code.gson.** { *; }
-keep interface com.google.code.gson.** { *; }


-keepclassmembers class **.R$* {
    public static <fields>;
}

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

-keep class com.google.android.material.** { *; }

-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**

-dontwarn androidx.**
-keep class androidx.** { *; }
-keep interface androidx.** { *; }


-keep class !android.support.v7.internal.view.menu.**,android.support.** {*;}
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

-keep public class * extends android.support.v4.** {*;}
-keep public class * extends android.app.Fragment


-keepclassmembers class ** {
    public void onEvent*(***);
}


#remove Logs
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}