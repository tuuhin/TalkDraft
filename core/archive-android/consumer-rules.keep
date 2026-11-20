# ==============================================================================
# 1. Keep Native Methods & JNI Bridge Classes
# ==============================================================================
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class com.sam.talkdraft.archive_android.NativeBzip2Compressor { *; }
-keep class com.sam.talkdraft.archive_android.NativeTarArchiver { *; }
-keep class com.sam.talkdraft.archive_android.ArchiveProgressListener { *; }
-keep class com.sam.talkdraft.archive_android.** { native <methods>; }

# ==============================================================================
# 2. Preserve Exceptions Thrown via JNI
# ==============================================================================
-keep class com.sam.talkdraft.archive_android.exceptions.NativeCompressionException {
    <init>(java.lang.String);
    <fields>;
    <methods>;
}

-keep class com.sam.talkdraft.archive_android.exceptions.NativeArchiveException {
    <init>(java.lang.String);
    <fields>;
    <methods>;
}

# ==============================================================================
# 3. Preserve Metadata, Attributes & Debugging Information
# ==============================================================================
-keepattributes Signature, InnerClasses, EnclosingMethod, Exceptions
