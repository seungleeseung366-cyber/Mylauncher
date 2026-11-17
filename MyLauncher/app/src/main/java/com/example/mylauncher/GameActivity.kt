package com.example.mylauncher

import android.app.Activity
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Toast
import com.example.mylauncher.launcher.JvmLauncher
import java.io.File
import kotlin.concurrent.thread

/** Proses terpisah (:game). Surface diserahkan ke lapisan native/GLFW. */
class GameActivity : Activity(), SurfaceHolder.Callback {

    private var started = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val surface = SurfaceView(this)
        surface.holder.addCallback(this)
        setContentView(surface)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (started) return
        started = true
        val version = intent.getStringExtra("version") ?: return
        val gameDir = File(intent.getStringExtra("gameDir") ?: return)
        thread {
            val err = JvmLauncher.start(this, version, gameDir, holder.surface)
            runOnUiThread {
                if (err != null) Toast.makeText(this, err, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) {}
}
