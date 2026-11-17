package com.example.mylauncher.launcher

import android.content.Context
import android.util.Log
import android.view.Surface
import java.io.File

object JvmLauncher {
    private const val TAG = "JvmLauncher"

    /** Return pesan error, atau null jika JVM selesai tanpa masalah. */
    fun start(ctx: Context, versionId: String, gameDir: File, surface: Surface): String? {
        val s = Settings(ctx)
        val jreHome = File(ctx.filesDir, "runtime/jre")
        val lwjglDir = File(ctx.filesDir, "lwjgl")

        if (!AssetZip.extract(ctx, "jre-arm64.zip", jreHome))
            return "assets/jre-arm64.zip tidak ada. Taruh JRE 17 Android arm64 di sana (lihat README)."
        val hasLwjgl = AssetZip.extract(ctx, "lwjgl-android.zip", lwjglDir)
        val lwjglJars = if (hasLwjgl) lwjglDir.listFiles { f -> f.extension == "jar" }?.toList().orEmpty() else emptyList()

        val args = LaunchArgs.build(versionId, gameDir, s.username, lwjglJars)
        val nativeDir = ctx.applicationInfo.nativeLibraryDir

        // Environment untuk renderer + library native
        NativeBridge.setenv("JAVA_HOME", jreHome.absolutePath)
        NativeBridge.setenv("HOME", gameDir.absolutePath)
        NativeBridge.setenv("LD_LIBRARY_PATH", "$nativeDir:${jreHome.absolutePath}/lib:${jreHome.absolutePath}/lib/server")
        NativeBridge.setenv("POJAV_RENDERER", s.renderer)

        // Muat library JRE yang saling bergantung (urutan penting)
        for (lib in listOf("lib/libjava.so", "lib/libnio.so", "lib/libnet.so", "lib/libverify.so")) {
            val f = File(jreHome, lib)
            if (f.exists()) NativeBridge.dlopenGlobal(f.absolutePath)
        }

        NativeBridge.setSurface(surface)

        val jvmArgs = listOf(
            "-Xms256m", "-Xmx${s.ramMb}m",
            "-Djava.home=${jreHome.absolutePath}",
            "-Djava.io.tmpdir=${ctx.cacheDir.absolutePath}",
            "-Duser.home=${gameDir.absolutePath}",
            "-Dorg.lwjgl.librarypath=$nativeDir",
            "-Djava.library.path=$nativeDir",
            "-Dos.name=Linux",
            "-Dminecraft.launcher.brand=MyLauncher",
            "-Dminecraft.launcher.version=0.2",
            "-Djava.class.path=${args.classpath}",
        )
        Log.i(TAG, "mainClass=${args.mainClass} renderer=${s.renderer}")

        val rc = NativeBridge.startJvm(jreHome.absolutePath, jvmArgs.toTypedArray(), args.mainClass, args.gameArgs.toTypedArray())
        return if (rc == 0) null else "JVM berhenti dengan kode $rc (cek logcat tag LauncherNative)"
    }
}
