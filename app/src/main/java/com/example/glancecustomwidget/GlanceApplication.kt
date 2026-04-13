package com.example.glancecustomwidget

import android.app.Application

/**
 * 自定义 Application 类。
 * 目前仅作为入口，后续可在此处初始化全局单例（例如 DI 容器）。
 * 需要在 AndroidManifest.xml 的 <application android:name=".GlanceApplication"> 中声明。
 */
class GlanceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 如有需要，可在此初始化全局资源
    }
}
