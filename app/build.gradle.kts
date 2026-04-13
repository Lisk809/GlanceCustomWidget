import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.parcelize")
}

// ── 签名配置：优先读取本地 keystore.properties，CI 通过 -P 参数传入 ──────────
//
// 两种使用场景：
//  [本地开发]  复制 keystore.properties.template → keystore.properties 并填写信息
//  [CI/CD]    通过 Gradle 命令行参数传入，例如：
//             ./gradlew assembleRelease \
//               -Pandroid.injected.signing.store.file=/path/to/release.jks \
//               -Pandroid.injected.signing.store.password=xxx \
//               -Pandroid.injected.signing.key.alias=xxx \
//               -Pandroid.injected.signing.key.password=xxx
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) load(FileInputStream(keystorePropsFile))
}

android {
    namespace  = "com.example.glancecustomwidget"
    compileSdk = 35

    defaultConfig {
        applicationId  = "com.example.glancecustomwidget"
        minSdk         = 26   // Glance AppWidget 要求 API 26+
        targetSdk      = 35
        versionCode    = 1
        versionName    = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // ── 签名配置 ──────────────────────────────────────────────────────────────
    signingConfigs {
        // 只有本地存在 keystore.properties 时才注册 release 签名配置；
        // CI 通过 -Pandroid.injected.signing.* 参数覆盖，不依赖此块
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile     = file(keystoreProps["storeFile"] as String)
                storePassword = keystoreProps["storePassword"] as String
                keyAlias      = keystoreProps["keyAlias"] as String
                keyPassword   = keystoreProps["keyPassword"] as String
            }
        }
    }

    buildTypes {
        debug {
            // Debug 包使用 .debug 后缀，可与 Release 包在同一设备并存
            applicationIdSuffix = ".debug"
            versionNameSuffix   = "-debug"
            isDebuggable        = true
        }

        release {
            isMinifyEnabled   = true    // 开启 R8 代码压缩/混淆
            isShrinkResources = true    // 移除未使用的资源，减小 APK 体积
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 本地有签名配置时使用；CI 通过 -Pandroid.injected.signing.* 覆盖
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose     = true
        buildConfig = true   // 允许代码中访问 BuildConfig.VERSION_NAME 等常量
    }

    // 打包时排除重复的 META-INF 文件（多个 Kotlin 库依赖时的常见冲突）
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ── Compose BOM：统一管理所有 Compose 库版本 ──────────────────────────────
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ── 导航 ──────────────────────────────────────────────────────────────────
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ── Activity / ViewModel / Lifecycle ─────────────────────────────────────
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")

    // ── Jetpack Glance（桌面小部件框架）──────────────────────────────────────
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance-material3:1.1.0")

    // ── DataStore（持久化存储小部件配置）──────────────────────────────────────
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ── kotlinx-serialization（配置对象 <-> JSON 字符串）────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    // ── 基础库 ────────────────────────────────────────────────────────────────
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // ── 单元测试 ──────────────────────────────────────────────────────────────
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
