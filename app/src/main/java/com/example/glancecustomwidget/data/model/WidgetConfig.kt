package com.example.glancecustomwidget.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * 单个桌面小部件的所有配置属性。
 *
 * @Serializable  → kotlinx-serialization 生成 JSON 序列化器，
 *                  用于将配置存入 Preferences DataStore（JSON 字符串）
 * @Parcelize     → kotlin-parcelize 插件生成 Parcelable 实现，
 *                  用于通过 Navigation 的 Bundle 传递（可选，也可用 widgetId 字符串导航）
 *
 * 颜色字段使用 Long 存储 ARGB 值（0xAARRGGBB 格式），
 * 例如不透明紫色 = 0xFF6200EE，不透明白色 = 0xFFFFFFFF。
 * 使用 Long 而非 Int 是为了避免符号位问题（颜色值 > 0x7FFFFFFF 时 Int 会溢出为负数）。
 */
@Parcelize
@Serializable
data class WidgetConfig(
    /** 内部唯一标识符（应用内生成，与系统 appWidgetId 不同） */
    val widgetId: String = UUID.randomUUID().toString(),

    /** 在应用列表中显示的名称，必填 */
    val name: String = "新小部件",

    /** 小部件桌面上显示的标题行（可留空） */
    val title: String = "",

    /** 小部件桌面上显示的正文内容（可留空） */
    val content: String = "Hello, Widget!",

    /** 背景颜色，ARGB Long，默认 Material Purple */
    val bgColor: Long = 0xFF6200EEL,

    /** 文字颜色，ARGB Long，默认白色 */
    val textColor: Long = 0xFFFFFFFFL,

    /** 字体大小（单位 sp） */
    val textSizeSp: Int = 14,

    /** 圆角大小（单位 dp），在 API 31+ 设备上生效 */
    val cornerRadiusDp: Int = 16
) : Parcelable
