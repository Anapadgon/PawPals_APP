# PawPals — reglas mínimas; amplía si activas minify en release.
-keepattributes Signature
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
