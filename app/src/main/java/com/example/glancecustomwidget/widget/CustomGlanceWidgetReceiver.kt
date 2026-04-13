package com.example.glancecustomwidget.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * CustomGlanceWidgetReceiver：系统与 Glance 小部件的通信入口。
 *
 * ── 继承链 ────────────────────────────────────────────────────────────────
 *  CustomGlanceWidgetReceiver
 *    └── GlanceAppWidgetReceiver（Glance 提供）
 *          └── AppWidgetProvider（Android 框架）
 *                └── BroadcastReceiver
 *
 * Glance 在 GlanceAppWidgetReceiver 内部已经处理了 onUpdate、onReceive 等回调，
 * 并自动调用 glanceAppWidget.update(context, glanceId) 来触发 provideGlance。
 * 我们只需：
 *  1. 声明 glanceAppWidget 返回我们的实现类
 *  2. 在 onDeleted 中清理不再需要的资源（可选）
 *
 * ── 生命周期回调时序 ──────────────────────────────────────────────────────
 *
 *  [首次添加小部件]
 *    onEnabled(context)          → 此类型的第一个实例被添加（之前无同类实例）
 *    onUpdate(context, manager, [appWidgetId])  → 立即被调用
 *    → Glance 内部 → provideGlance(context, GlanceId) → 小部件渲染
 *
 *  [再次添加同类型小部件]
 *    onUpdate(context, manager, [新appWidgetId])  → 只调用 onUpdate，不调用 onEnabled
 *    → Glance 内部 → provideGlance → 小部件渲染
 *
 *  [小部件从桌面移除]
 *    onDeleted(context, [appWidgetId])
 *    （若已无同类实例）onDisabled(context)
 *
 *  [系统重启 / 桌面重启]
 *    onUpdate 被调用（系统要求重绘所有小部件）
 *    → Glance 读取之前保存的 PreferencesGlanceState → 恢复配置 → 重新渲染
 *    这就是为什么要把 widgetId 存入 Glance PreferencesGlanceState 而非内存。
 */
class CustomGlanceWidgetReceiver : GlanceAppWidgetReceiver() {

    /**
     * 告诉 GlanceAppWidgetReceiver 使用哪个 GlanceAppWidget 实现。
     * Glance 框架会在合适的时机调用此对象的 provideGlance 方法。
     */
    override val glanceAppWidget: GlanceAppWidget = CustomGlanceWidget()

    // 用于 Receiver 中的异步操作（Receiver 的 onReceive 在主线程，不能直接调用挂起函数）
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * onEnabled：同类型小部件的第一个实例被添加到桌面时调用。
     * 此时 onUpdate 随后会立即调用，无需在此重复初始化。
     */
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // 可在此处做一次性初始化，如创建通知渠道等
    }

    /**
     * onDeleted：一个或多个该类型小部件实例被从桌面移除时调用。
     *
     * appWidgetIds：被删除的小部件 ID 数组。
     * 我们在这里清理 Glance 的 PreferencesGlanceState，
     * 但保留 DataStore 中的 WidgetConfig（用户可能会重新添加该配置的小部件）。
     *
     * 注意：GlanceAppWidgetReceiver 的父类已调用了 super 所需的逻辑，
     * 这里的 super.onDeleted 会触发 Glance 清理内部状态文件。
     */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        // Glance 会自动清理对应的 PreferencesGlanceState 文件
        // DataStore 中的 WidgetConfig 被保留，供用户将来重新添加或查看
    }

    /**
     * onDisabled：所有同类型小部件实例都被移除时调用。
     * 可在此处释放全局资源。
     */
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
    }

    /**
     * 在 Receiver 销毁时取消协程作用域，防止泄漏。
     * 注意：BroadcastReceiver 生命周期极短，scope 主要用于防止任务无限延续。
     */
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // GlanceAppWidgetReceiver.onReceive 已处理所有标准广播
        // 如需监听自定义广播，在此扩展
    }
}
