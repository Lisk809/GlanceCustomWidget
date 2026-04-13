package com.example.glancecustomwidget.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.glancecustomwidget.data.model.WidgetConfig
import com.example.glancecustomwidget.data.repository.WidgetConfigRepository
import com.example.glancecustomwidget.widget.WidgetConfigManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * MainViewModel：主界面（列表）的状态持有者。
 *
 * 继承 AndroidViewModel（而非 ViewModel）是为了方便获取 applicationContext，
 * 用于：
 *  - 初始化 WidgetConfigRepository（需要 Context 创建 DataStore）
 *  - 在删除配置后调用 WidgetConfigManager 更新 Glance 小部件
 *
 * 数据流：
 *  DataStore（Preferences）→ Repository.allConfigs（Flow）
 *    → stateIn（转为 StateFlow）→ WidgetListScreen（collectAsStateWithLifecycle）
 *
 * 使用 StateFlow 而非直接使用 Flow 的好处：
 *  - StateFlow 有初始值（不会在第一次 collect 前挂起 UI）
 *  - StateFlow 缓存最新值（旋转屏幕后立即可用）
 *  - WhileSubscribed(5000)：UI 进入后台 5 秒后停止收集，节省资源
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WidgetConfigRepository(application)

    /**
     * 所有小部件配置的列表状态。
     * 每当 DataStore 中的数据变化（保存/删除配置），此 Flow 自动发射新列表。
     */
    val widgetConfigs: StateFlow<List<WidgetConfig>> = repository.allConfigs
        .stateIn(
            scope            = viewModelScope,
            started          = SharingStarted.WhileSubscribed(5_000L),
            initialValue     = emptyList()
        )

    /**
     * 删除指定配置，并更新所有使用该配置的桌面小部件。
     *
     * 流程：
     *  1. 从 DataStore 删除配置
     *  2. 遍历所有 Glance 小部件实例，找到使用此 widgetId 的，触发重新渲染
     *     → 由于配置已删除，CustomGlanceWidget.provideGlance 读取时会返回 null
     *     → 但目前实现在 getConfigForGlanceId 返回 null 时仍会创建新默认配置
     *     → 为了展示"配置已删除"提示，可以修改 provideGlance 中的逻辑
     *     → 这里简化为直接更新所有小部件（让它们重新渲染）
     */
    fun deleteConfig(widgetId: String) {
        viewModelScope.launch {
            // 先更新 DataStore
            repository.deleteConfig(widgetId)
            // 再刷新桌面小部件（使用 applicationContext）
            WidgetConfigManager.updateAllGlanceWidgets(getApplication())
        }
    }
}
