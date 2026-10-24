# 官方要求：源码公开（登记客户端时填的仓库）。
-keepnames class app.shixing.client.data.** { *; }
-keepattributes *Annotation*, InnerClasses
-dontwarn kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class app.shixing.client.data.**$$serializer { *; }
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# WorkManager 靠类名反射实例化 Worker。
-keep class app.shixing.client.work.** { *; }
