# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the SDK tools.

# Moshi
-keep class com.gym.app.data.entity.** { *; }
-keep class com.gym.app.data.importer.** { *; }
# 计划分享串模型（SharedPlan/SharedExercise/LegacyPlan/LegacyExercise 嵌套类）：
# KotlinJsonAdapterFactory 反射读取构造参数，R8 混淆会导致 release 包导入失败
-keep class com.gym.app.data.planshare.** { *; }
