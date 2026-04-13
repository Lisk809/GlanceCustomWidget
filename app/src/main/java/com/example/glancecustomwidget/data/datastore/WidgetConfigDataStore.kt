package com.example.glancecustomwidget.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.glancecustomwidget.data.model.WidgetConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// ── DataStore 文件级单例扩展 ──────────────────────────────────────────────────
//
// `by preferencesDataStore(name)` 是 Kotlin 委托属性，它在 Context 类上创建单例
// DataStore 实例。只要始终通过 applicationContext 访问，就能保证全局唯一。
//
// 【重要】同一进程中不能为同一文件名创建多个 DataStore 实例，否则抛异常。
// 将扩展声明为 private 顶层属性，可防止外部代码意外创建额外实例。

/** 存储所有小部件配置（JSON 列表字符串）*/
private val Context.configDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "widget_configs")

/**
 * 统一访问 DataStore 的工具类。
 * 始终通过 [context.applicationContext] 访问，确保 DataStore 单例。
 *
 * 存储结构：
 *  - configDataStore["all_configs"] = JSON 数组字符串，包含所有 [WidgetConfig]
 */
class WidgetConfigDataStore(private val context: Context) {

    // 始终使用 applicationContext 避免 Activity/Service 泄漏
    private val store get() = context.applicationContext.configDataStore

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    companion object {
        /** DataStore Preferences 中存储配置列表的键 */
        val CONFIGS_KEY = stringPreferencesKey("all_configs")
    }

    // ── 读取操作 ──────────────────────────────────────────────────────────────

    /**
     * 获取所有小部件配置的响应式 Flow。
     * Flow 在每次 DataStore 写入时自动发射新值，UI 层通过 collectAsState() 观察。
     */
    val allConfigs: Flow<List<WidgetConfig>> = store.data.map { prefs ->
        parseConfigList(prefs[CONFIGS_KEY])
    }

    /**
     * 挂起函数：一次性读取单个配置（非响应式）。
     * 在 Glance 的 provideGlance（suspend 上下文）中使用。
     */
    suspend fun getConfig(widgetId: String): WidgetConfig? {
        val prefs = store.data.first()
        return parseConfigList(prefs[CONFIGS_KEY]).find { it.widgetId == widgetId }
    }

    /** 一次性读取全部配置（suspend）*/
    suspend fun getAllConfigsOnce(): List<WidgetConfig> {
        val prefs = store.data.first()
        return parseConfigList(prefs[CONFIGS_KEY])
    }

    // ── 写入操作 ──────────────────────────────────────────────────────────────

    /**
     * 保存或更新一个配置。
     * 如果列表中已有相同 widgetId 的配置，则替换；否则追加到末尾。
     */
    suspend fun saveConfig(config: WidgetConfig) {
        store.edit { prefs ->
            val current = parseConfigList(prefs[CONFIGS_KEY])
            // 过滤掉旧版本（如有），然后追加新版本
            val updated = current.filter { it.widgetId != config.widgetId } + config
            prefs[CONFIGS_KEY] = json.encodeToString(updated)
        }
    }

    /**
     * 删除指定 widgetId 的配置。
     * 注意：已在桌面上使用此配置的小部件将显示"配置已删除"的提示。
     */
    suspend fun deleteConfig(widgetId: String) {
        store.edit { prefs ->
            val current = parseConfigList(prefs[CONFIGS_KEY])
            val updated = current.filter { it.widgetId != widgetId }
            prefs[CONFIGS_KEY] = json.encodeToString(updated)
        }
    }

    // ── 私有工具 ──────────────────────────────────────────────────────────────

    private fun parseConfigList(jsonStr: String?): List<WidgetConfig> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<WidgetConfig>>(jsonStr)
        } catch (e: Exception) {
            // JSON 格式损坏时优雅降级，返回空列表而非崩溃
            emptyList()
        }
    }
}
