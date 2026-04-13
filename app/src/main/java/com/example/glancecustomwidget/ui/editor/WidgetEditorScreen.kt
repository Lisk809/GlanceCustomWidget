package com.example.glancecustomwidget.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.glancecustomwidget.R
import com.example.glancecustomwidget.data.model.WidgetConfig
import com.example.glancecustomwidget.utils.ColorUtils

/**
 * 小部件编辑界面。
 *
 * 包含：
 *  - 顶部 App Bar（返回 + 保存按钮）
 *  - 实时预览卡片（随参数变化即时更新）
 *  - 表单字段：名称、标题、内容
 *  - 颜色选择器（背景色 & 文字颜色，预设色板）
 *  - 滑块：字号 & 圆角
 *  - 底部"添加到桌面"提示
 *
 * @param widgetId  路由参数："new" = 创建，否则为编辑的 UUID
 * @param onSaved   保存成功后的回调（导航返回）
 * @param onBack    点击返回按钮的回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetEditorScreen(
    widgetId  : String,
    onSaved   : () -> Unit,
    onBack    : () -> Unit,
    viewModel : WidgetEditorViewModel = viewModel()
) {
    val config   by viewModel.config.collectAsStateWithLifecycle()
    val uiState  by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    // ── 副作用：响应 UiState 变化 ─────────────────────────────────────────
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is WidgetEditorViewModel.UiState.SavedOk -> onSaved()
            is WidgetEditorViewModel.UiState.Error   -> {
                snackbar.showSnackbar(state.message)
                viewModel.clearError()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (viewModel.isNew)
                            stringResource(R.string.editor_title_new)
                        else
                            stringResource(R.string.editor_title_edit)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 保存按钮（右上角）
                    if (uiState == WidgetEditorViewModel.UiState.Saving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 16.dp)
                        )
                    } else {
                        IconButton(
                            onClick = viewModel::save,
                            enabled = uiState is WidgetEditorViewModel.UiState.Ready ||
                                      uiState is WidgetEditorViewModel.UiState.Error
                        ) {
                            Icon(
                                imageVector         = Icons.Default.Check,
                                contentDescription  = stringResource(R.string.editor_btn_save),
                                tint                = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPadding ->
        // 加载中显示 Loading
        if (uiState == WidgetEditorViewModel.UiState.Loading) {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── 实时预览 ────────────────────────────────────────────────────
            SectionLabel("预览")
            WidgetLivePreview(config = config)

            // ── 基础信息 ────────────────────────────────────────────────────
            SectionLabel("基础信息")

            OutlinedTextField(
                value          = config.name,
                onValueChange  = viewModel::updateName,
                label          = { Text(stringResource(R.string.editor_field_name)) },
                modifier       = Modifier.fillMaxWidth(),
                singleLine     = true,
                isError        = config.name.isBlank()
            )

            OutlinedTextField(
                value         = config.title,
                onValueChange = viewModel::updateTitle,
                label         = { Text(stringResource(R.string.editor_field_title)) },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true
            )

            OutlinedTextField(
                value         = config.content,
                onValueChange = viewModel::updateContent,
                label         = { Text(stringResource(R.string.editor_field_content)) },
                modifier      = Modifier.fillMaxWidth(),
                minLines      = 2,
                maxLines      = 4
            )

            // ── 背景颜色 ────────────────────────────────────────────────────
            SectionLabel(stringResource(R.string.editor_section_bg_color))
            ColorPicker(
                selectedColor = config.bgColor,
                colorOptions  = ColorUtils.PRESET_BG_COLORS,
                onColorSelect = viewModel::updateBgColor
            )

            // ── 文字颜色 ────────────────────────────────────────────────────
            SectionLabel(stringResource(R.string.editor_section_text_color))
            ColorPicker(
                selectedColor = config.textColor,
                colorOptions  = ColorUtils.PRESET_TEXT_COLORS,
                onColorSelect = viewModel::updateTextColor
            )

            // ── 字号滑块 ────────────────────────────────────────────────────
            SectionLabel(stringResource(R.string.editor_section_text_size, config.textSizeSp))
            Slider(
                value         = config.textSizeSp.toFloat(),
                onValueChange = { viewModel.updateTextSizeSp(it.toInt()) },
                valueRange    = 8f..32f,
                steps         = 23  // 8→32，步长 1sp
            )

            // ── 圆角滑块 ────────────────────────────────────────────────────
            SectionLabel(stringResource(R.string.editor_section_corner, config.cornerRadiusDp))
            Slider(
                value         = config.cornerRadiusDp.toFloat(),
                onValueChange = { viewModel.updateCornerRadiusDp(it.toInt()) },
                valueRange    = 0f..32f,
                steps         = 31  // 0→32，步长 1dp
            )

            // ── 保存按钮（底部大按钮） ────────────────────────────────────
            Button(
                onClick   = viewModel::save,
                modifier  = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled   = uiState is WidgetEditorViewModel.UiState.Ready ||
                            uiState is WidgetEditorViewModel.UiState.Error,
                shape     = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text     = stringResource(R.string.editor_btn_save),
                    fontSize = 16.sp
                )
            }

            // ── 添加到桌面提示 ──────────────────────────────────────────────
            AddToDesktopHint()

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── 子组件 ──────────────────────────────────────────────────────────────────

/** 实时预览卡片：模拟小部件在桌面上的显示效果 */
@Composable
private fun WidgetLivePreview(config: WidgetConfig) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        shape     = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier          = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(config.cornerRadiusDp.dp))
                .background(Color(config.bgColor)),
            contentAlignment  = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier.padding(16.dp)
            ) {
                if (config.title.isNotBlank()) {
                    Text(
                        text       = config.title,
                        color      = Color(config.textColor),
                        fontSize   = (config.textSizeSp + 2).sp,
                        fontWeight = FontWeight.Bold,
                        textAlign  = TextAlign.Center,
                        maxLines   = 1
                    )
                }
                if (config.content.isNotBlank()) {
                    Text(
                        text      = config.content,
                        color     = Color(config.textColor),
                        fontSize  = config.textSizeSp.sp,
                        textAlign = TextAlign.Center,
                        maxLines  = 2
                    )
                }
                if (config.title.isBlank() && config.content.isBlank()) {
                    Text(
                        text      = config.name,
                        color     = Color(config.textColor),
                        fontSize  = config.textSizeSp.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * 颜色预设选择器。
 * 水平滚动列表，每个色块是圆形按钮；当前选中项显示白色对勾。
 */
@Composable
private fun ColorPicker(
    selectedColor : Long,
    colorOptions  : List<Pair<String, Long>>,
    onColorSelect : (Long) -> Unit
) {
    LazyRow(
        contentPadding      = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(colorOptions) { (name, colorLong) ->
            val isSelected = selectedColor == colorLong
            Box(
                modifier         = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(colorLong))
                    .then(
                        if (isSelected)
                            Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        else
                            Modifier.border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                    )
                    .clickable { onColorSelect(colorLong) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    // 选中时显示对勾，颜色自动与背景对比
                    val checkColor = if (ColorUtils.recommendTextColor(colorLong) == 0xFFFFFFFFL)
                        Color.White else Color.Black
                    Icon(
                        imageVector        = Icons.Default.Check,
                        contentDescription = name,
                        tint               = checkColor,
                        modifier           = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/** 区块标签文字 */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text       = text,
        style      = MaterialTheme.typography.labelLarge,
        color      = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Medium
    )
}

/** 底部"如何添加到桌面"提示卡片 */
@Composable
private fun AddToDesktopHint() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape  = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector        = Icons.Default.Info,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.secondary,
                modifier           = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text  = stringResource(R.string.editor_hint_desktop),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
