package com.example.glancecustomwidget.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * 颜色相关工具函数。
 *
 * 颜色在整个项目中以 Long 形式流通（0xAARRGGBB 格式），
 * 因为这是最通用的表示方式：
 *   - 能用于 Compose：Color(longValue)
 *   - 能用于 Glance：ColorProvider(Color(longValue))
 *   - 能用于 Android View：视图颜色通常用 Int，可以 toLong()
 *   - 能被 kotlinx-serialization 直接序列化
 */
object ColorUtils {

    /** 将 Long ARGB 颜色值转为 Compose Color */
    fun Long.toComposeColor(): Color = Color(this)

    /** 将 Compose Color 转为 Long ARGB 颜色值 */
    fun Color.toLongColor(): Long = this.toArgb().toLong() and 0xFFFFFFFFL

    /**
     * 颜色预设表，用于背景颜色选择器。
     * 格式：Pair(颜色名称, Long颜色值)
     */
    val PRESET_BG_COLORS: List<Pair<String, Long>> = listOf(
        "深紫"   to 0xFF6200EEL,
        "靛蓝"   to 0xFF3700B3L,
        "海蓝"   to 0xFF1565C0L,
        "天蓝"   to 0xFF0288D1L,
        "青色"   to 0xFF00838FL,
        "翠绿"   to 0xFF2E7D32L,
        "橄榄"   to 0xFF558B2FL,
        "琥珀"   to 0xFFE65100L,
        "砖红"   to 0xFFB71C1CL,
        "玫红"   to 0xFFC62828L,
        "深棕"   to 0xFF4E342EL,
        "深灰"   to 0xFF37474FL,
        "炭黑"   to 0xFF212121L,
        "纯白"   to 0xFFFFFFFFL,
    )

    /**
     * 颜色预设表，用于文字颜色选择器。
     */
    val PRESET_TEXT_COLORS: List<Pair<String, Long>> = listOf(
        "纯白"   to 0xFFFFFFFFL,
        "炭黑"   to 0xFF212121L,
        "浅灰"   to 0xFFBDBDBDL,
        "淡黄"   to 0xFFFFF176L,
        "浅绿"   to 0xFFA5D6A7L,
        "浅蓝"   to 0xFF90CAF9L,
        "浅橙"   to 0xFFFFCC80L,
    )

    /**
     * 计算文字在给定背景色上的推荐对比色（黑色或白色）。
     * 使用相对亮度（relative luminance）公式，符合 WCAG 2.0 标准。
     */
    fun recommendTextColor(bgColor: Long): Long {
        val color = Color(bgColor)
        // 计算相对亮度（linearize sRGB → luminance）
        fun linearize(c: Float): Double =
            if (c <= 0.04045) c / 12.92 else Math.pow(((c + 0.055) / 1.055).toDouble(), 2.4)

        val r = linearize(color.red)
        val g = linearize(color.green)
        val b = linearize(color.blue)
        val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b

        // 亮度 > 0.179 用深色文字，否则用白色文字
        return if (luminance > 0.179) 0xFF212121L else 0xFFFFFFFFL
    }
}
