package com.example.mylauncher.launcher

import org.json.JSONObject
import java.io.File
import java.net.URL

/**
 * Mengunduh: version JSON, client.jar, libraries, asset index + assets.
 * Versi sederhana: library dengan aturan OS (rules) dilewati.
 */
object VersionInstaller {

    fun install(v: McVersion, gameDir: File, onProgress: (String, Int) -> Unit) {
        val vDir = File(gameDir, "versions/${v.id}")
        val jsonFile = File(vDir, "${v.id}.json")
        vDir.mkdirs()
        if (!jsonFile.exists()) jsonFile.writeText(URL(v.url).readText())
        val json = JSONObject(jsonFile.readText())

        // 1. client.jar
        onProgress("Mengunduh client.jar", 5)
        val client = json.getJSONObject("downloads").getJSONObject("client")
        Downloader.download(client.getString("url"), File(vDir, "${v.id}.jar"))

        // 2. libraries
        val libs = json.getJSONArray("libraries")
        for (i in 0 until libs.length()) {
            val lib = libs.getJSONObject(i)
            if (lib.has("rules")) continue
            val artifact = lib.optJSONObject("downloads")?.optJSONObject("artifact") ?: continue
            onProgress("Library ${i + 1}/${libs.length()}", 5 + 35 * i / libs.length())
            Downloader.download(artifact.getString("url"), File(gameDir, "libraries/${artifact.getString("path")}"))
        }

        // 3. assets
        val idx = json.getJSONObject("assetIndex")
        val idxFile = File(gameDir, "assets/indexes/${idx.getString("id")}.json")
        Downloader.download(idx.getString("url"), idxFile)
        val objects = JSONObject(idxFile.readText()).getJSONObject("objects")
        val names = objects.keys().asSequence().toList()
        names.forEachIndexed { i, name ->
            val hash = objects.getJSONObject(name).getString("hash")
            val sub = hash.substring(0, 2)
            if (i % 50 == 0) onProgress("Assets ${i}/${names.size}", 40 + 60 * i / names.size)
            Downloader.download(
                "https://resources.download.minecraft.net/$sub/$hash",
                File(gameDir, "assets/objects/$sub/$hash")
            )
        }
        onProgress("Selesai", 100)
    }
}
