package com.example.mylauncher.launcher

import android.content.Context
import java.io.File
import java.util.zip.ZipInputStream

object AssetZip {
    /** Ekstrak assets/[name] ke [dest]. Return false jika asset tidak ada. */
    fun extract(ctx: Context, name: String, dest: File, marker: String = ".done"): Boolean {
        val done = File(dest, marker)
        if (done.exists()) return true
        val input = try { ctx.assets.open(name) } catch (e: Exception) { return false }
        dest.mkdirs()
        ZipInputStream(input.buffered()).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                val f = File(dest, e.name)
                if (!f.canonicalPath.startsWith(dest.canonicalPath)) throw SecurityException("zip slip")
                if (e.isDirectory) f.mkdirs() else {
                    f.parentFile?.mkdirs()
                    f.outputStream().use { zip.copyTo(it) }
                    f.setExecutable(true, false)
                }
                e = zip.nextEntry
            }
        }
        done.writeText("ok")
        return true
    }
}
