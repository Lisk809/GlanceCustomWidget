package com.example.glancecustomwidget.data.repository

import android.content.Context
import com.example.glancecustomwidget.data.datastore.WidgetConfigDataStore
import com.example.glancecustomwidget.data.model.WidgetConfig
import kotlinx.coroutines.flow.Flow

/**
 * Repository 层：对上层（ViewModel、Widget）屏蔽具体存储实现细节。
 *
 * 当前实现基于 DataStore；若将来迁移到 Room，只需替换此类内部实现，
 * ViewModel 和 Widget 代码无需改动。
 *
 * ViewModel 通过此类的 Flow 观察数据变化，UI 自动更新。
 * Glance Widget 通过挂起函数一次性读取配置。
 */
class WidgetConfigRepository(context: Context) {

    private val dataStore = WidgetConfigDataStore(context)

    // ── 响应式查询（给 ViewModel 用）─────────────────────────────────────────

    /** 所有配置的实时流，UI 层 collectAsState() 订阅 */
    val allConfigs: Flow<List<WidgetConfig>> = dataStore.allConfigs

    // ── 挂起函数（给 Glance Widget 用）──────────────────────────────────────

    /** 根据 widgetId 获取单个配置（在 Glance provideGlance 的协程中调用） */
    suspend fun getConfig(widgetId: String): WidgetConfig? =
        dataStore.getConfig(widgetId)

    /** 一次性获取所有配置 */
    suspend fun getAllConfigsOnce(): List<WidgetConfig> =
        dataStore.getAllConfigsOnce()

    // ── 写操作（给 ViewModel 和 Receiver 用）────────────────────────────────

    /** 保存或更新配置 */
    suspend fun saveConfig(config: WidgetConfig) = dataStore.saveConfig(config)

    /** 删除配置 */
    suspend fun deleteConfig(widgetId: String) = dataStore.deleteConfig(widgetId)
}
