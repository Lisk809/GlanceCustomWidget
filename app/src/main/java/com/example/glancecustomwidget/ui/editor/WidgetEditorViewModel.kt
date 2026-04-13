package com.example.glancecustomwidget.ui.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.glancecustomwidget.data.model.WidgetConfig
import com.example.glancecustomwidget.data.repository.WidgetConfigRepository
import com.example.glancecustomwidget.widget.WidgetConfigManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * WidgetEditorViewModel：编辑界面的状态管理。
 *
 * 通过 [SavedStateHandle] 获取导航参数（widgetId），
 * 以便在进程死亡后恢复状态（系统内存回收后重开仍能正确加载数据）。
 *
 * 状态设计：
 *   _config（MutableStateFlow）← 编辑界面双向绑定的数据源
 *   config（StateFlow）       → 暴露给 UI 层（只读）
 *   _uiState                 → 通知 UI 是否保存成功 / 是否加载中
 */
class WidgetEditorViewModel(
    application       : Application,
    savedStateHandle  : SavedStateHandle
) : AndroidViewModel(application) {

    private val repository = WidgetConfigRepository(application)

    /** 路由传入的 widgetId；"new" 表示创建新配置 */
    val widgetId: String = savedStateHandle["widgetId"] ?: "new"
    val isNew: Boolean get() = widgetId == "new"

    // ── 配置状态 ──────────────────────────────────────────────────────────

    private val _config = MutableStateFlow(WidgetConfig())
    val config: StateFlow<WidgetConfig> = _config.asStateFlow()

    // ── UI 状态 ───────────────────────────────────────────────────────────

    sealed class UiState {
        object Loading  : UiState()
        object Ready    : UiState()
        object Saving   : UiState()
        object SavedOk  : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // ── 初始化：加载现有配置 ──────────────────────────────────────────────

    init {
        viewModelScope.launch {
            if (!isNew) {
                val existing = repository.getConfig(widgetId)
                if (existing != null) {
                    _config.value = existing
                } else {
                    // 配置不存在（可能已被删除），退化为新建模式
                    _config.value = WidgetConfig()
                }
            }
            // 无论新建还是编辑，加载完成后转为 Ready
            _uiState.value = UiState.Ready
        }
    }

    // ── 字段更新方法（每个字段独立更新，避免 UI 层直接操作不可变数据类）──

    fun updateName(name: String)             { _config.value = _config.value.copy(name = name) }
    fun updateTitle(title: String)           { _config.value = _config.value.copy(title = title) }
    fun updateContent(content: String)       { _config.value = _config.value.copy(content = content) }
    fun updateBgColor(bgColor: Long)         { _config.value = _config.value.copy(bgColor = bgColor) }
    fun updateTextColor(textColor: Long)     { _config.value = _config.value.copy(textColor = textColor) }
    fun updateTextSizeSp(size: Int)          { _config.value = _config.value.copy(textSizeSp = size) }
    fun updateCornerRadiusDp(corner: Int)    { _config.value = _config.value.copy(cornerRadiusDp = corner) }

    // ── 保存 ──────────────────────────────────────────────────────────────

    /**
     * 保存当前配置，并触发对应桌面小部件的重新渲染。
     *
     * 流程：
     *  1. 校验必填字段（名称）
     *  2. 写入 DataStore
     *  3. 调用 WidgetConfigManager.updateGlanceWidgetsForConfig 刷新桌面
     *  4. 设置 UiState.SavedOk → UI 层导航返回
     *
     * 关于步骤 3 的说明：
     *   - 如果用户还没有把小部件添加到桌面，getGlanceIds 会返回空列表，
     *     updateGlanceWidgetsForConfig 什么都不做，是安全的。
     *   - 如果用户已添加小部件到桌面，则找到匹配的 GlanceId 并触发 update()，
     *     update() 内部重新调用 provideGlance，读取刚写入 DataStore 的最新配置。
     */
    fun save() {
        val current = _config.value
        if (current.name.isBlank()) {
            _uiState.value = UiState.Error("名称不能为空")
            return
        }

        _uiState.value = UiState.Saving
        viewModelScope.launch {
            try {
                repository.saveConfig(current)

                // 刷新桌面上所有使用此配置的 Glance 小部件
                WidgetConfigManager.updateGlanceWidgetsForConfig(
                    context  = getApplication(),
                    widgetId = current.widgetId
                )

                _uiState.value = UiState.SavedOk
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "保存失败")
            }
        }
    }

    /** 清除错误状态，恢复为 Ready */
    fun clearError() {
        _uiState.value = UiState.Ready
    }
}
