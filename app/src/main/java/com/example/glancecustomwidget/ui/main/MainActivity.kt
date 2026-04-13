package com.example.glancecustomwidget.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.glancecustomwidget.ui.editor.WidgetEditorScreen
import com.example.glancecustomwidget.ui.list.WidgetListScreen
import com.example.glancecustomwidget.ui.theme.GlanceCustomWidgetTheme

/**
 * MainActivity：应用的唯一 Activity，承载所有 Compose 导航目的地。
 *
 * 导航结构（Navigation Compose）：
 *   "list"                → WidgetListScreen（小部件列表）
 *   "editor/new"          → WidgetEditorScreen（新建模式）
 *   "editor/{widgetId}"   → WidgetEditorScreen（编辑模式）
 *
 * 当从桌面点击小部件跳转到此 Activity 时，Intent 会携带 EXTRA_WIDGET_ID，
 * onCreate / onNewIntent 中检查并导航到对应配置的编辑界面。
 */
class MainActivity : ComponentActivity() {

    companion object {
        /** 从 Glance 小部件点击跳转时，Intent 携带此 Extra 以直接打开编辑界面 */
        const val EXTRA_WIDGET_ID = "extra_widget_id"
    }

    // 用于在 Compose 层外部触发导航（例如 onNewIntent）
    private var pendingWidgetId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 提取来自 Glance 小部件点击的 widgetId
        pendingWidgetId = intent?.getStringExtra(EXTRA_WIDGET_ID)

        setContent {
            GlanceCustomWidgetTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(startWidgetId = pendingWidgetId)
                }
            }
        }
    }

    /**
     * 当 Activity 已存在（launchMode = singleTop）且从桌面点击小部件时，
     * 系统不会重新创建 Activity，而是调用 onNewIntent。
     * 需要将新的 widgetId 传递给 Compose 导航。
     */
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // 实际项目中可通过 SharedFlow 或 ViewModel 将此 ID 传入 Compose 层
        // 这里为简化展示仅更新 pendingWidgetId，重新 setContent 不是最佳实践
        setIntent(intent)
    }
}

// ── Compose 导航图 ──────────────────────────────────────────────────────────

/**
 * 应用的导航宿主。
 * @param startWidgetId 如果从小部件点击进入，此值非空，直接导航到编辑界面
 */
@Composable
private fun AppNavigation(startWidgetId: String? = null) {
    val navController = rememberNavController()

    // 根据是否有 startWidgetId 决定起始目的地
    val startDestination = if (startWidgetId != null) {
        "editor/$startWidgetId"
    } else {
        "list"
    }

    NavHost(
        navController    = navController,
        startDestination = startDestination
    ) {
        // ── 列表界面 ────────────────────────────────────────────────────────
        composable("list") {
            WidgetListScreen(
                onCreateNew  = { navController.navigate("editor/new") },
                onEditWidget = { widgetId -> navController.navigate("editor/$widgetId") }
            )
        }

        // ── 编辑界面 ────────────────────────────────────────────────────────
        // widgetId = "new" 表示创建新配置，否则为编辑现有配置的 UUID
        composable(
            route     = "editor/{widgetId}",
            arguments = listOf(
                navArgument("widgetId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val widgetId = backStackEntry.arguments?.getString("widgetId") ?: "new"
            WidgetEditorScreen(
                widgetId  = widgetId,
                onSaved   = { navController.popBackStack() },
                onBack    = { navController.popBackStack() }
            )
        }
    }
}
