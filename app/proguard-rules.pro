# 保留 kotlinx-serialization 相关类（防止混淆时序列化失败）
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# 保留 WidgetConfig 数据类（序列化用）
-keep class com.example.glancecustomwidget.data.model.** { *; }
