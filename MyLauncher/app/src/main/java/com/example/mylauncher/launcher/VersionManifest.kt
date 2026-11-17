package com.example.mylauncher.launcher

import org.json.JSONObject
import java.net.URL

data class McVersion(val id: String, val type: String, val url: String)

object VersionManifest {
    private const val MANIFEST_URL =
        "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"

    fun fetchVersions(): List<McVersion> {
        val json = JSONObject(URL(MANIFEST_URL).readText())
        val arr = json.getJSONArray("versions")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            McVersion(o.getString("id"), o.getString("type"), o.getString("url"))
        }
    }
}
