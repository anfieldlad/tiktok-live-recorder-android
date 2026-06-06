# kotlinx.serialization keeps generated serializers; the plugin emits the rules,
# but keep our @Serializable models defensively.
-keepclassmembers class com.ttldownloader.app.** {
    *** Companion;
}
-keep @kotlinx.serialization.Serializable class com.ttldownloader.app.** { *; }
