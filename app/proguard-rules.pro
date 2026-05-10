# Исправленные правила для собственных классов с учётом реальных пакетов
-keep class com.example.project5.data.model.Receipt { *; }
-keep class com.example.project5.data.model.ExpenseItem { *; }   # вместо Product
-keep class com.example.project5.domain.statistics.CategoryStatistic { *; }

# Gson – можно сузить до необходимых пакетов (оставляем, но предупреждения игнорируем)
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# OkHttp и Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# JSON (org.json) – не требует keep, только dontwarn
-dontwarn org.json.**

# Coroutines
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# MPAndroidChart
-dontwarn com.github.mikephil.charting.**
-keep class com.github.mikephil.charting.** { *; }

# Подавление предупреждений о платформенных классах
-dontwarn javax.annotation.**
-dontwarn javax.net.**
-dontwarn com.squareup.okhttp3.internal.platform.**

# Репозиторий (если нужен)
-keep class com.example.project5.data.repository.ReceiptRepository { *; }

# Общее правило для всех классов проекта (если необходимо сохранить имена)
# Можно заменить на более точное: -keep class com.example.project5.** { <init>(...); }
-keepclassmembers class com.example.project5.** {
    <init>(...);
}

# Если используются какие-то модели, которые сериализуются Gson, лучше добавить:
-keep class com.example.project5.data.model.** { *; }
-keep class com.example.project5.domain.** { *; }   # осторожно, может быть широким