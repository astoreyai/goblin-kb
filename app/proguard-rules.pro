# KB Keyboard ProGuard Rules

# Keep serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep KB model classes
-keep,includedescendantclasses class dev.kymera.keyboard.**$$serializer { *; }
-keepclassmembers class dev.kymera.keyboard.** {
    *** Companion;
}
-keepclasseswithmembers class dev.kymera.keyboard.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep layout and command configs
-keep class dev.kymera.keyboard.layouts.** { *; }
-keep class dev.kymera.keyboard.commands.** { *; }
-keep class dev.kymera.keyboard.settings.** { *; }

# Keep InputMethodService
-keep class dev.kymera.keyboard.core.KBKeyboardService { *; }
