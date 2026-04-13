package com.example.glancecustomwidget.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.wrapContentHeight
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.glancecustomwidget.data.model.WidgetConfig
import com.example.glancecustomwidget.data.repository.WidgetConfigRepository
import com.example.glancecustomwidget.ui.main.MainActivity

/**
 * CustomGlanceWidget：核心 Glance 小部件实现。
 *
 * ── Glance 生命周期简介 ────────────────────────────────────────────────────
 *
 *  用户添加小部件到桌面
 *       │
 *       ▼
 *  系统分配 appWidgetId（Int）
 *  → CustomGlanceWidgetReceiver.onUpdate() 被调用
 *  → Glance 内部调用 provideGlance(context, GlanceId)
 *       │
 *       ▼
 *  provideGlance() [协程] 执行：
 *    1. 检查 Glance Preferences 中是否已有 widgetId 映射
 *    2. 没有 → 创建默认 WidgetConfig，写入 DataStore 和 Glance 状态
 *    3. 有   → 从 DataStore 读取对应 WidgetConfig
 *    4. 调用 provideContent { WidgetContent(config) }
 *       │
 *       ▼
 *  WidgetContent() [Glance Composable] 根据 config 渲染 RemoteViews
 *       │
 *       ▼
 *  系统将 RemoteViews 显示在桌面
 *
 *  当用户在应用内修改配置并保存时：
 *    WidgetConfigManager.updateGlanceWidgetsForConfig(context, widgetId)
 *    → 找到对应 GlanceId → CustomGlanceWidget().update(context, glanceId)
 *    → provideGlance 重新执行 → 读取新配置 → 重新渲染 → 桌面更新
 *
 * ── stateDefinition 说明 ──────────────────────────────────────────────────
 *
 *  stateDefinition = PreferencesGlanceStateDefinition 告诉 Glance：
 *  使用 Preferences DataStore 作为此 Widget 的状态存储。
 *  每个 GlanceId（每个桌面实例）有独立的 Preferences 存储空间。
 *  我们在其中存储 internal_widget_id → 内部 UUID，从而关联到 WidgetConfig。
 */
class CustomGlanceWidget : GlanceAppWidget() {

    /**
     * 声明使用 Preferences 作为 Glance 的状态存储类型。
     * Glance 会为每个小部件实例维护独立的 Preferences，
     * 我们利用它存储 internal_widget_id（内部配置 UUID）。
     */
    override val stateDefinition = PreferencesGlanceStateDefinition

    /**
     * provideGlance：小部件内容的主要入口。
     *
     * 这是一个 suspend 函数，运行在 Glance 管理的协程中，因此可以：
     * - 读取 DataStore（挂起操作）
     * - 执行耗时计算
     * 在调用 provideContent 之前完成所有数据加载。
     *
     * @param context 系统提供的 Context
     * @param id      本次渲染的 GlanceId（对应一个桌面小部件实例）
     */
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = WidgetConfigRepository(context)

        // ── 步骤 1：检查是否已有 widgetId 绑定 ───────────────────────────
        var config = WidgetConfigManager.getConfigForGlanceId(context, id)

        if (config == null) {
            // ── 步骤 2：首次加载（此 GlanceId 尚未绑定配置） ─────────────
            // 创建默认配置并保存，然后将 widgetId 写入 Glance Preferences
            val defaultConfig = WidgetConfig(
                name    = "我的小部件",
                title   = "自定义小部件",
                content = "点击打开应用来编辑我！"
            )
            repository.saveConfig(defaultConfig)
            // 建立 GlanceId → widgetId 的映射（存入 Glance Preferences 状态）
            WidgetConfigManager.bindWidgetId(context, id, defaultConfig.widgetId)
            config = defaultConfig
        }

        // ── 步骤 3：调用 provideContent 提供 Glance Composable 内容 ─────
        // provideContent 是阻塞式的（直到小部件被系统移除）
        // Glance 会将 composable lambda 转换为 RemoteViews 并发送给桌面
        provideContent {
            GlanceTheme {
                WidgetContent(
                    config     = config,
                    glanceId   = id,
                    appContext  = context
                )
            }
        }
    }
}

// ── Glance Composable ──────────────────────────────────────────────────────
//
// 注意：这里使用的是 Glance 的 UI 组件（Box、Column、Text 等），
// 不是 Compose 的同名组件！两套 API 相似但有差异：
//   - 修饰符使用 GlanceModifier，不是 Modifier
//   - Text 的样式用 TextStyle（来自 androidx.glance.text）
//   - 颜色通过 ColorProvider 包裹（支持深色/浅色主题适配）
//   - 不支持 Canvas、自定义 Layout 等复杂 Compose 特性

/**
 * 桌面小部件的 Glance Composable 内容。
 *
 * Glance Composable 最终会被序列化为 Android RemoteViews，
 * 因此只能使用 Glance 提供的有限组件集合。
 */
@androidx.compose.runtime.Composable
private fun WidgetContent(
    config    : WidgetConfig,
    glanceId  : GlanceId,
    appContext : Context
) {
    // 构建点击时跳转到 MainActivity 的 Intent，携带 widgetId 参数
    // 这样 MainActivity 可以直接打开对应配置的编辑界面
    val openIntent = Intent(appContext, MainActivity::class.java).apply {
        putExtra(MainActivity.EXTRA_WIDGET_ID, config.widgetId)
        // FLAG_ACTIVITY_NEW_TASK 在非 Activity 上下文（Widget/BroadcastReceiver）中必须设置
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    // 构建根 Modifier：填满小部件区域、设置背景色、圆角、点击动作
    val rootModifier = GlanceModifier
        .fillMaxSize()
        .background(ColorProvider(Color(config.bgColor)))
        .clickable(actionStartActivity(openIntent))
        .then(
            // cornerRadius 在 API 31（Android 12）以上原生支持；
            // Glance 1.1.0 在低版本会忽略此修饰符，不会崩溃
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                GlanceModifier.cornerRadius(config.cornerRadiusDp.dp)
            } else {
                GlanceModifier
            }
        )

    Box(
        modifier           = rootModifier,
        contentAlignment   = Alignment.Center
    ) {
        Column(
            modifier          = GlanceModifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题行（如果非空才显示）
            if (config.title.isNotBlank()) {
                Text(
                    text  = config.title,
                    style = TextStyle(
                        color      = ColorProvider(Color(config.textColor)),
                        fontSize   = (config.textSizeSp + 2).sp,  // 标题比正文大 2sp
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }

            // 正文内容（如果非空才显示）
            if (config.content.isNotBlank()) {
                Text(
                    text  = config.content,
                    style = TextStyle(
                        color    = ColorProvider(Color(config.textColor)),
                        fontSize = config.textSizeSp.sp
                    ),
                    maxLines = 3
                )
            }

            // 如果标题和内容都为空，显示占位提示
            if (config.title.isBlank() && config.content.isBlank()) {
                Text(
                    text  = config.name,
                    style = TextStyle(
                        color      = ColorProvider(Color(config.textColor)),
                        fontSize   = config.textSizeSp.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}
