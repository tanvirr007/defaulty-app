# Defaulty ProGuard/R8 Rules
# The default Android optimization rules handle most cases.
# Add project-specific rules here as needed.

# Keep SupportedRole enum for serialization in navigation args
-keepclassmembers enum app.defaulty.domain.model.SupportedRole {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep preference enums (names are persisted as strings in DataStore)
-keepclassmembers enum app.defaulty.data.preferences.ApplyMode {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers enum app.defaulty.data.preferences.ThemeMode {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Shizuku
-keep class * extends rikka.shizuku.ShizukuProvider { *; }
-keep class rikka.shizuku.** { *; }
-keep class dev.rikka.shizuku.** { *; }
