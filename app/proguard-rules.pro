# Keep kotlinx.serialization generated serializers.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.passwordassistant.app.**$$serializer { *; }
-keepclassmembers class com.passwordassistant.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.passwordassistant.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
