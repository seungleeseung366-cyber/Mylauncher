package com.example.mylauncher.launcher

import java.io.File
import java.net.URL

object Downloader {
    /** Download file jika belum ada. */
    fun download(url: String, dest: File) {
        if (dest.exists() && dest.length() > 0) return
        dest.parentFile?.mkdirs()
        val tmp = File(dest.path + ".part")
        URL(url).openStream().use { input ->
            tmp.outputStream().use { out -> input.copyTo(out) }
        }
        tmp.renameTo(dest)
    }
}
