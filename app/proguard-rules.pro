# Andruav app keep rules.
#
# Release currently builds with minifyEnabled false, so R8 does not run and
# these rules are inert. They exist so that turning minification on does not
# break the WebRTC stack: org.webrtc classes are called from the native
# libjingle_peerconnection_so.so through JNI (@CalledByNative), so their
# names and members must survive shrinking/obfuscation.

-keep class org.webrtc.** { *; }
-keep interface org.webrtc.** { *; }
-keep enum org.webrtc.** { *; }
-keepclasseswithmembernames class org.webrtc.** { *; }

# JNI in general: any method declared native anywhere must keep its name and
# signature, and classes holding native methods must keep their names too.
-keepclasseswithmembernames class * {
    native <methods>;
}
