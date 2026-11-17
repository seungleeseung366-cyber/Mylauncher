package com.example.mylauncher.launcher

import android.content.Context

class Settings(ctx: Context) {
    private val p = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
    var username: String
        get() = p.getString("username", "Player") ?: "Player"
        set(v) = p.edit().putString("username", v.ifBlank { "Player" }).apply()
    var ramMb: Int
        get() = p.getInt("ram", 1024)
        set(v) = p.edit().putInt("ram", v.coerceIn(512, 8192)).apply()
    /** gl4es | zink | angle  (nama library renderer di jniLibs) */
    var renderer: String
        get() = p.getString("renderer", "gl4es") ?: "gl4es"
        set(v) = p.edit().putString("renderer", v).apply()
}
