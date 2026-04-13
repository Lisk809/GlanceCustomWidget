package com.example.glancecustomwidget.ui.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.glancecustomwidget.R
import com.example.glancecustomwidget.data.model.WidgetConfig
import com.example.glancecustomwidget.ui.main.MainViewModel

/**
 * 小部件列表界面（应用首屏）。
 *
 * 状态来源：MainViewModel.widgetConfigs（StateFlow<List<WidgetConfig>>）
 * 事件流向：
 *   - 点击 FAB          → onCreateNew()   → 导航到新建编辑界面
 *   - 点击列表项        → onEditWidget()  → 导航到编辑界面
 *   - 长按列表项        → 显示删除确认对话框
 *   - 确认删除          → viewModel.deleteConfig()
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetListScreen(
    onCreateNew  : () -> Unit,
    onEditWidget : (String) -> Unit,
    viewModel    : MainViewModel = viewModel()
) {
    // collectAsStateWithLifecycle：在 Compose 中订阅 Flow，
    // 生命周期感知（Activity 进入后台时停止收集，避免不必要的更新）
    val configs by viewModel.widgetConfigs.collectAsStateWithLifecycle()

    // 待删除的配置（非 null 时显示确认对话框）
    var configToDelete by remember { mutableStateOf<WidgetConfig?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = stringResource(R.string.list_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick          = onCreateNew,
                containerColor   = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector         = Icons.Default.Add,
                    contentDescription  = stringResource(R.string.fab_create),
                    tint                = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (configs.isEmpty()) {
                // ── 空状态视图 ──────────────────────────────────────────────
                EmptyStateView(modifier = Modifier.align(Alignment.Center))
            } else {
                // ── 配置列表 ────────────────────────────────────────────────
                LazyColumn(
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items   = configs,
                        key     = { it.widgetId }   // 稳定 key：列表项动画更流畅
                    ) { config ->
                        WidgetConfigCard(
                            config    = config,
                            onClick   = { onEditWidget(config.widgetId) },
                            onLongClick = { configToDelete = config }
                        )
                    }
                    // 底部空白，防止 FAB 遮挡最后一项
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    // ── 删除确认对话框 ──────────────────────────────────────────────────────
    configToDelete?.let { config ->
        AlertDialog(
            onDismissRequest = { configToDelete = null },
            icon = {
                Icon(Icons.Default.Delete, contentDescription = null,
                    tint = MaterialTheme.colorScheme.error)
            },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text  = {
                Text(
                    text = stringResource(R.string.delete_confirm_message, config.name)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteConfig(config.widgetId)
                        configToDelete = null
                    }
                ) {
                    Text(
                        text  = stringResource(R.string.delete_confirm_ok),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { configToDelete = null }) {
                    Text(stringResource(R.string.delete_confirm_cancel))
                }
            }
        )
    }
}

// ── 子组件 ──────────────────────────────────────────────────────────────────

/**
 * 单个小部件配置的卡片组件。
 * 左侧：色块预览（模拟桌面小部件的视觉效果）
 * 右侧：名称、标题/内容摘要
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WidgetConfigCard(
    config     : WidgetConfig,
    onClick    : () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick      = onClick,
                onLongClick  = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape     = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment   = Alignment.CenterVertically
        ) {
            // ── 颜色预览方块 ──────────────────────────────────────────────
            WidgetPreviewBlock(config = config)

            Spacer(modifier = Modifier.width(16.dp))

            // ── 文字信息 ──────────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = config.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 16.sp,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                if (config.title.isNotBlank() || config.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text     = listOf(config.title, config.content)
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * 小部件预览色块：以圆角矩形展示小部件的背景色、标题和内容，
 * 让用户在列表中快速辨认。
 */
@Composable
private fun WidgetPreviewBlock(config: WidgetConfig) {
    Box(
        modifier          = Modifier
            .size(width = 80.dp, height = 52.dp)
            .clip(RoundedCornerShape(config.cornerRadiusDp.dp.coerceAtMost(12.dp)))
            .background(Color(config.bgColor)),
        contentAlignment  = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(4.dp)
        ) {
            if (config.title.isNotBlank()) {
                Text(
                    text     = config.title,
                    color    = Color(config.textColor),
                    fontSize = (config.textSizeSp - 4).coerceAtLeast(8).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (config.content.isNotBlank()) {
                Text(
                    text     = config.content,
                    color    = Color(config.textColor).copy(alpha = 0.85f),
                    fontSize = (config.textSizeSp - 5).coerceAtLeast(7).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** 列表为空时的中间提示视图 */
@Composable
private fun EmptyStateView(modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector        = Icons.Default.Widgets,
            contentDescription = null,
            modifier           = Modifier.size(64.dp),
            tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text      = stringResource(R.string.list_empty),
            style     = MaterialTheme.typography.bodyLarge,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
