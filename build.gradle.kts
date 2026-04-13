// 顶层 build 文件：声明所有子模块共用的插件（apply false = 不在此处应用，由子模块按需应用）
plugins {
    id("com.android.application")        version "8.5.0"  apply false
    id("org.jetbrains.kotlin.android")   version "2.0.0"  apply false
    id("org.jetbrains.kotlin.plugin.compose")       version "2.0.0"  apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"  apply false
    id("org.jetbrains.kotlin.plugin.parcelize")     version "2.0.0"  apply false

    // 依赖版本更新检查插件（供 dependency-check.yml workflow 使用）
    // 运行：./gradlew dependencyUpdates -Drevision=release
    id("com.github.ben-manes.versions") version "0.51.0" apply false
}
