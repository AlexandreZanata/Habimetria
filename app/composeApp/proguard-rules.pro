# Keep kotlinx.serialization and Ktor on release builds.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class kotlinx.serialization.** { *; }
-keep class io.ktor.** { *; }
-dontwarn kotlinx.serialization.**
