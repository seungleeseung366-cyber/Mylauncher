package com.example.mylauncher.launcher

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

object LaunchArgs {

    data class Result(val mainClass: String, val classpath: String, val gameArgs: List<String>)

    /** Evaluasi "rules" Mojang. OS dianggap linux (Android = kernel Linux). */
    fun rulesAllow(rules: JSONArray?): Boolean {
        if (rules == null) return true
        var allowed = false
        for (i in 0 until rules.length()) {
            val r = rules.getJSONObject(i)
            val os = r.optJSONObject("os")
            val features = r.optJSONObject("features")
            val matches = (os == null || os.optString("name") == "linux") && features == null
            if (matches) allowed = r.getString("action") == "allow"
        }
        return allowed
    }

    fun build(versionId: String, gameDir: File, username: String, lwjglJars: List<File>): Result {
        val vDir = File(gameDir, "versions/$versionId")
        val json = JSONObject(File(vDir, "$versionId.json").readText())

        val cp = mutableListOf<String>()
        cp += lwjglJars.map { it.absolutePath }       // LWJGL versi Android didahulukan
        val libs = json.getJSONArray("libraries")
        for (i in 0 until libs.length()) {
            val lib = libs.getJSONObject(i)
            if (!rulesAllow(lib.optJSONArray("rules"))) continue
            val path = lib.optJSONObject("downloads")?.optJSONObject("artifact")?.optString("path") ?: continue
            if (lwjglJars.isNotEmpty() && path.startsWith("org/lwjgl")) continue // diganti
            cp += File(gameDir, "libraries/$path").absolutePath
        }
        cp += File(vDir, "$versionId.jar").absolutePath

        val raw = mutableListOf<String>()
        if (json.has("arguments")) {
            val arr = json.getJSONObject("arguments").getJSONArray("game")
            for (i in 0 until arr.length()) {
                val a = arr.get(i)
                if (a is String) raw += a
            }
        } else raw += json.getString("minecraftArguments").split(" ")

        val vars = mapOf(
            "\${auth_player_name}" to username,
            "\${version_name}" to versionId,
            "\${game_directory}" to gameDir.absolutePath,
            "\${assets_root}" to File(gameDir, "assets").absolutePath,
            "\${game_assets}" to File(gameDir, "assets").absolutePath,
            "\${assets_index_name}" to json.getJSONObject("assetIndex").getString("id"),
            "\${auth_uuid}" to UUID.nameUUIDFromBytes("OfflinePlayer:$username".toByteArray()).toString(),
            "\${auth_access_token}" to "0",
            "\${clientid}" to "0",
            "\${auth_xuid}" to "0",
            "\${user_type}" to "legacy",
            "\${version_type}" to json.optString("type", "release"),
        )
        val args = raw.map { a -> vars.entries.fold(a) { s, (k, v) -> s.replace(k, v) } }
        return Result(json.getString("mainClass"), cp.joinToString(":"), args)
    }
}
