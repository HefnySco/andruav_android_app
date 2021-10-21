# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\Program Files (x86)\Android\android-studio\sdk/tools/proguard/proguard-android.txt
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

#-flattenpackagehierarchy 'myobfuscated'
#-allowaccessmodification


#-dontobfuscate
-dontwarn net.tsz.afinal.**
-dontwarn hoho.**
-dontwarn de.greenrobot.**
-dontwarn org.codehaus.**
-dontwarn java.**
-dontwarn a.a.b.**
-dontwarn dji.**
-dontwarn com.dji.**
-dontwarn okio.**  #https://github.com/square/okio/issues/60
-dontwarn org.bouncycastle.**
-dontwarn org.apache.**
-dontwarn com.squareup.**
-dontwarn com.o3dr.**
-dontwarn boofcv.**
-dontwarn deepboof.io.DeepBoofDataBaseOps.**
-dontwarn org.codehaus.jackson.**
-dontwarn android.support.**

-dontwarn org.xmlpull.v1.**
-dontwarn com.github.sarxos.**
-dontwarn org.yaml.snakeyaml.**
-dontwarn com.thoughtworks.xstream.**
-dontwarn com.sun.jna.**
-dontwarn org.bytedeco.javacv.**
-dontwarn com.googlecode.mp4parser.authoring.tracks.mjpeg.OneJpegPerIframe.**
-dontwarn com.jcraft.jsch.jcraft.**
-dontwarn android.net.http.**
-dontwarn org.apache.http.**
-dontwarn org.apache.commons.codec.**
-dontwarn com.googlecode.mp4parser.**
-dontwarn org.ietf.jgss.**
-dontwarn org.droidplanner.**
-dontwarn org.ejml.**
-dontwarn org.bridj.**
-dontwarn org.slf4j.**
-dontwarn org.bytedeco.javacpp.tools.**
-keepattributes *Annotation*,EnclosingMethod,Signature,InnerClasses,Deprecated,Exceptions,EnclosingMethod


-keepclasseswithmembernames class * {
    native <methods>;
}


-keep class **       {  public *;  }
-keep interface **   {  public *;  }
-keep enum **        {  public *;  }

-keep class         junit.** { *; }
-keep class         net.tsz.afinal.** { *; }

-keep class         org.codehaus.jackson.** { *; }
-keep interface     org.codehaus.jackson.** { *; }
-keep enum          org.codehaus.jackson.** { *; }


-keep class         com.hoho.** { *; }
-keep interface     com.hoho.** { *; }
-keep enum          com.hoho.** { *; }

-keep class         com.o3dr.** { *; }
-keep interface     com.o3dr.** { *; }
-keep enum          com.o3dr.** { *; }

-keep class         com.getkeepsafe.dexcount.** { *; }
-keep interface     com.getkeepsafe.dexcount.** { *; }
-keep enum          com.getkeepsafe.dexcount.** { *; }

-keep class         a.** { *; }
-keep interface     a.** { *; }
-keep enum          a.** { *; }

-keep class         dji.** { *; }
-keep interface     dji.** { *; }
-keep enum          dji.** { *; }

-keep class         com.dji.** { *; }
-keep interface     com.dji.** { *; }
-keep enum          com.dji.** { *; }

-keep class         okio.** { *; }

-keep class         hoho.** { *; }
-keep interface     hoho.** { *; }

-keep class         org.bouncycastle.** { *; }
-keep interface     org.bouncycastle.** { *; }

-keep class         com.google.vending.** { *; }
-keep interface     com.google.vending.** { *; }

-keep class         com.google.android.gms.** { *; }
-keep interface     com.google.android.gms.** { *; }

-keep class         com.google.gson.** { *; }
-keep interface     com.google.gson.** { *; }

-keep class         boofcv.core.** { *; }
-keep class         boofcv.abst.** { *; }
-keep class         boofcv.factory.** { *; }
-keep class         boofcv.struct.** { *; }
-keep interface     boofcv.** { *; }
-keep enum          boofcv.** { *; }
################### TEST
-keep class         org.bytedeco.javacpp.** { *; }
-keep interface     org.bytedeco.javacpp.** { *; }
-keep class         org.bytedeco.javacv.** { *; }
-keep interface     org.bytedeco.javacv.** { *; }
-keep class         org.bytedeco.javacpp.** { *; }
-keep interface     org.bytedeco.javacpp.** { *; }

-keep class         org.apache.http.** { *; }
-keep interface     org.apache.http.** { *; }
-keep enum          org.apache.http.** { *; }
-keep class         android.net.http.** { *; }



-keep class         com.sun.jna.** { *; }
-keep interface     com.sun.jna.** { *; }
-keep enum          com.sun.jna.** { *; }

-keep class         com.MAVLink.** { *; }
-keep interface     com.MAVLink.** { *; }
-keep enum          com.MAVLink.** { *; }

-keep class org.xmlpull.** { *; }
-keep class com.jogamp.opencl.** { *; }
-keep class com.jogamp.opengl.** { *; }
-keep class com.coremedia.iso.boxes.Container {*;}
-keep class com.jcraft.jsch.jce.**  { *; }
-keep class * extends com.jcraft.jsch.KeyExchange
-keep class com.jcraft.jsch.**  { *; }
-keep class com.jcraft.jzlib.ZStream
-keep class org.ietf.jgss.**  { *; }
-keep class com.jcraft.jsch.jce.**
-keep class com.jcraft.jsch.JSch.**
-keep class com.jcraft.jsch.**

-keep class java.nio.file.** {*;}
-keep interface com.jcraft.jsch.**  { *; }
-keep class * extends com.jcraft.jsch.KeyExchange

-keep class         org.droidplanner.** { *; }
-keep interface     org.droidplanner.** { *; }
-keep enum          org.droidplanner.** { *; }


################### TEST END

-keep class com.google.android.material.** { *; }

-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**

-dontwarn androidx.**
-keep class androidx.** { *; }
-keep interface androidx.** { *; }


-keep class         android.support.v4.** { *; }
-keep interface     android.support.v4.** { *; }
-keep class         android.support.v7.** { *; }
-keep interface     android.support.v7.** { *; }



-keep class         android.support.design.** { *; }
-keep interface     android.support.design.** { *; }


-keep class     android.support.** { *; }
-keep interface android.support.** { *; }

-keep class !android.support.v7.internal.view.menu.*MenuBuilder*, android.support.v7.** { *; }
-keep interface android.support.v7.** { *; }
-keep class !android.support.v7.internal.view.menu.**,** {*;}


-keep class !android.support.v7.internal.view.menu.**,android.support.** {*;}
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

-keep public class * extends android.support.v4.** {*;}
-keep public class * extends android.app.Fragment



-keep class javax.** { *; }
-keep interface javax.** { *; }
-keep class java.** { *; }
-keep interface java.** { *; }


#-keep public class     ** { public *;                            protected *; }
#-keep enum      ** { public *;                    }
#-keep interface ** { public *;                     }




#remove Logs
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}