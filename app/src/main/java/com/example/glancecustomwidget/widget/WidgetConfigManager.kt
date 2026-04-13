package com.example.glancecustomwidget.widget

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.example.glancecustomwidget.data.model.WidgetConfig
import com.example.glancecustomwidget.data.repository.WidgetConfigRepository

/**
 * WidgetConfigManager：应用层与 Glance 框架之间的桥梁。
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  两套 ID 系统说明                                                │
 * │                                                                 │
 * │  appWidgetId（Int）：系统分配，每次用户添加小部件到桌面时生成，  │
 * │                       可通过 AppWidgetManager 查询。            │
 * │                                                                 │
 * │  widgetId（String）：应用内部生成（UUID），关联到 WidgetConfig。 │
 * │                       存储在 Glance 的 PreferencesGlanceState  │
 * │                       中，键为 WIDGET_ID_PREFS_KEY。            │
 * │                                                                 │
 * │  映射关系：                                                      │
 * │    appWidgetId ──► GlanceId（通过 GlanceAppWidgetManager）      │
 * │    GlanceId    ──► widgetId（通过读 Glance Preferences 状态）   │
 * │    widgetId    ──► WidgetConfig（通过 Repository/DataStore）     │
 * └─────────────────────────────────────────────────────────────────┘
 */
object WidgetConfigManager {

    /**
     * 存在 Glance PreferencesGlanceState 中的键，值为该小部件实例对应的内部 widgetId。
     * 每个 GlanceId（即每个桌面小部件实例）有独立的 Preferences 存储空间。
     */
    val WIDGET_ID_PREFS_KEY = stringPreferencesKey("internal_widget_id")

    // ── 读取操作 ──────────────────────────────────────────────────────────────

    /**
     * 根据 GlanceId 读取该小部件实例对应的 WidgetConfig。
     * 在 [CustomGlanceWidget.provideGlance] 中调用（已在协程上下文）。
     *
     * 流程：
     *  1. 从 Glance Preferences 状态读取 widgetId（String）
     *  2. 用 widgetId 从 DataStore 查询 WidgetConfig
     *  3. 如果 widgetId 为空（首次加载），返回 null（调用方负责创建默认配置）
     */
    suspend fun getConfigForGlanceId(context: Context, glanceId: GlanceId): WidgetConfig? {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        val widgetId = prefs[WIDGET_ID_PREFS_KEY] ?: return null
        return WidgetConfigRepository(context).getConfig(widgetId)
    }

    /**
     * 将内部 widgetId 写入 Glance 的 Preferences 状态，建立映射。
     * 在首次创建默认配置时调用。
     */
    suspend fun bindWidgetId(context: Context, glanceId: GlanceId, widgetId: String) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[WIDGET_ID_PREFS_KEY] = widgetId
            }
        }
    }

    /**
     * 查找所有使用指定 widgetId 的 Glance 实例，并触发它们重新渲染。
     *
     * 在用户于应用内修改配置后调用，流程：
     *  1. 获取本应用所有小部件实例的 GlanceId 列表
     *  2. 对每个 GlanceId，读取其 Preferences 状态中的 widgetId
     *  3. 如果匹配，调用 CustomGlanceWidget().update(context, glanceId)
     *     触发 provideGlance 重新执行，读取最新配置并刷新桌面显示
     *
     * @param context  需传入 applicationContext
     * @param widgetId 内部配置 ID（UUID）
     */
    suspend fun updateGlanceWidgetsForConfig(context: Context, widgetId: String) {
        val manager = GlanceAppWidgetManager(context)
        // getGlanceIds 返回所有当前活跃的此类型小部件实例
        val allGlanceIds = manager.getGlanceIds(CustomGlanceWidget::class.java)

        allGlanceIds.forEach { glanceId ->
            val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
            if (prefs[WIDGET_ID_PREFS_KEY] == widgetId) {
                // 找到匹配的实例，触发更新
                // update() 会重新调用 provideGlance，从而读取最新的 WidgetConfig
                CustomGlanceWidget().update(context, glanceId)
            }
        }
    }

    /**
     * 强制刷新所有小部件（例如删除配置后，让已删除配置的小部件显示错误提示）
     */
    suspend fun updateAllGlanceWidgets(context: Context) {
        CustomGlanceWidget().updateAll(context)
    }
}
