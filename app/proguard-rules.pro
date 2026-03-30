# Quill ProGuard Rules
-keepattributes *Annotation*
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class com.quill.data.remote.dto.** { *; }
