# GlanceCustomWidget

[![Android CI](https://github.com/YOUR_USERNAME/GlanceCustomWidget/actions/workflows/build.yml/badge.svg)](https://github.com/YOUR_USERNAME/GlanceCustomWidget/actions/workflows/build.yml)

> 一个演示性质的 Android 应用，使用 **Jetpack Glance** 实现可在应用内自由配置的桌面小部件。

---

## 功能

- 📋 在应用内创建、编辑、删除桌面小部件配置
- 🎨 自定义背景色、文字颜色、字号、圆角等属性
- 👁️ 实时预览效果
- 🖥️ 通过 Jetpack Glance 渲染到系统桌面
- 💾 配置使用 Preferences DataStore 持久化存储
- 🔄 修改配置后自动刷新桌面小部件

---

## 技术栈

| 层次 | 技术 |
|------|------|
| UI（应用内） | Jetpack Compose + Material3 |
| UI（桌面小部件） | Jetpack Glance 1.1.0 |
| 导航 | Navigation Compose |
| 状态管理 | ViewModel + StateFlow |
| 持久化 | Preferences DataStore |
| 序列化 | kotlinx-serialization-json |
| 语言 | Kotlin 2.0 |
| 最低 SDK | API 26（Android 8.0） |

---

## 本地构建

### 前置要求

- Android Studio Koala（2024.1）或更高
- JDK 17
- Android SDK API 35

### Debug 构建

```bash
git clone https://github.com/YOUR_USERNAME/GlanceCustomWidget.git
cd GlanceCustomWidget
./gradlew assembleDebug
# APK 路径：app/build/outputs/apk/debug/app-debug.apk
```

### Release 构建（需要签名）

1. 复制签名模板：
   ```bash
   cp keystore.properties.template keystore.properties
   ```

2. 编辑 `keystore.properties`，填入你的 Keystore 信息：
   ```properties
   storeFile=../release.jks
   storePassword=your_password
   keyAlias=your_alias
   keyPassword=your_key_password
   ```

3. 构建：
   ```bash
   ./gradlew assembleRelease
   # APK 路径：app/build/outputs/apk/release/app-release.apk
   ```

---

## GitHub Actions CI/CD

项目包含两个 Workflow：

### `build.yml` — 主 CI/CD 流水线

| 触发条件 | 执行的 Job |
|----------|-----------|
| push 到 `main` / `develop` / PR | Lint + Debug APK 构建 |
| 推送 `v*.*.*` tag | Lint + Release APK 构建 + 自动创建 GitHub Release |

### `dependency-check.yml` — 依赖更新检查

每周一自动检查是否有新版本依赖可用，生成报告 Artifact。

### 配置 Release 签名（必须在推送 tag 前完成）

在仓库的 **Settings → Secrets and variables → Actions** 中添加以下 Secrets：

| Secret 名称 | 说明 |
|------------|------|
| `KEYSTORE_BASE64` | `.jks` 文件的 base64 编码内容 |
| `KEYSTORE_PASSWORD` | Keystore 密码 |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key 密码 |

**生成 base64 字符串：**

```bash
# macOS
base64 -i release.jks | pbcopy   # 内容已复制到剪贴板

# Linux
base64 release.jks

# Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.jks"))
```

### 发布新版本

```bash
# 1. 更新 app/build.gradle.kts 中的 versionCode 和 versionName
# 2. 提交并推送
git add .
git commit -m "chore: bump version to 1.1.0"
git push

# 3. 打 tag，自动触发 Release 构建
git tag v1.1.0
git push origin v1.1.0
```

几分钟后，GitHub Releases 页面会自动出现新 Release 并附上签名 APK。

---

## 项目结构

```
app/src/main/java/com/example/glancecustomwidget/
├── data/
│   ├── model/WidgetConfig.kt           # 配置数据类（@Serializable + @Parcelize）
│   ├── datastore/WidgetConfigDataStore.kt  # DataStore 读写
│   └── repository/WidgetConfigRepository.kt
├── widget/
│   ├── CustomGlanceWidget.kt           # Glance 小部件核心实现
│   ├── CustomGlanceWidgetReceiver.kt   # 系统广播接收器
│   └── WidgetConfigManager.kt          # appWidgetId ↔ widgetId 映射管理
├── ui/
│   ├── main/{MainActivity, MainViewModel}.kt
│   ├── list/WidgetListScreen.kt
│   ├── editor/{WidgetEditorScreen, WidgetEditorViewModel}.kt
│   └── theme/Theme.kt
└── utils/ColorUtils.kt
```

---

## Glance 工作原理简述

```
用户添加小部件到桌面
    │
    ▼
系统分配 appWidgetId（Int）
    │
    ▼
CustomGlanceWidgetReceiver.onUpdate()
    │  （Glance 内部处理）
    ▼
CustomGlanceWidget.provideGlance(context, GlanceId)  ← suspend 函数
    │  读取 Glance PreferencesState 中存储的 widgetId（UUID）
    │  用 widgetId 从 DataStore 查询 WidgetConfig
    ▼
provideContent { WidgetContent(config) }
    │  Glance 将 Composable 转换为 RemoteViews
    ▼
桌面显示小部件

用户在应用内修改配置并保存
    │
    ▼
WidgetConfigManager.updateGlanceWidgetsForConfig(context, widgetId)
    │  遍历所有 GlanceId，找到匹配的实例
    ▼
CustomGlanceWidget().update(context, glanceId)
    │  重新执行 provideGlance
    ▼
桌面小部件自动刷新
```

---

## License

MIT
