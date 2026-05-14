# Billing SDK — consumer ProGuard rules (merged into app when minify is enabled)

# Play Billing Library
-keep class com.android.billingclient.api.** { *; }

# Keep Hilt-generated entry points referenced from the SDK
-keep,allowobfuscation @interface dagger.hilt.android.EntryPoint
-keep @dagger.hilt.android.EntryPoint class * { *; }

# DataStore generated serializers
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}
