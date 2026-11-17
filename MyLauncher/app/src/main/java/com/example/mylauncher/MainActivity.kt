package com.example.mylauncher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
import com.example.mylauncher.launcher.*
import java.io.File
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private var versions: List<McVersion> = emptyList()
    private val renderers = listOf("gl4es", "zink", "angle")
    private lateinit var spinner: Spinner
    private lateinit var spinnerRenderer: Spinner
    private lateinit var editUser: EditText
    private lateinit var editRam: EditText
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private lateinit var settings: Settings

    private val gameDir: File get() = File(getExternalFilesDir(null), "minecraft")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        settings = Settings(this)

        spinner = findViewById(R.id.spinnerVersion)
        spinnerRenderer = findViewById(R.id.spinnerRenderer)
        editUser = findViewById(R.id.editUser)
        editRam = findViewById(R.id.editRam)
        status = findViewById(R.id.txtStatus)
        progress = findViewById(R.id.progress)

        editUser.setText(settings.username)
        editRam.setText(settings.ramMb.toString())
        spinnerRenderer.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, renderers)
        spinnerRenderer.setSelection(renderers.indexOf(settings.renderer).coerceAtLeast(0))

        loadVersions()
        findViewById<Button>(R.id.btnDownload).setOnClickListener { downloadSelected() }
        findViewById<Button>(R.id.btnPlay).setOnClickListener { play() }
    }

    private fun saveSettings() {
        settings.username = editUser.text.toString()
        settings.ramMb = editRam.text.toString().toIntOrNull() ?: 1024
        settings.renderer = renderers[spinnerRenderer.selectedItemPosition]
    }

    private fun loadVersions() = thread {
        try {
            val list = VersionManifest.fetchVersions().filter { it.type == "release" }
            runOnUiThread {
                versions = list
                spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, list.map { it.id })
                status.text = "Pilih versi lalu tekan Download"
            }
        } catch (e: Exception) {
            runOnUiThread { status.text = "Gagal memuat versi: ${e.message}" }
        }
    }

    private fun downloadSelected() {
        val v = versions.getOrNull(spinner.selectedItemPosition) ?: return
        thread {
            try {
                VersionInstaller.install(v, gameDir) { msg, pct ->
                    runOnUiThread { status.text = msg; progress.progress = pct }
                }
                runOnUiThread { status.text = "Selesai: ${v.id}" }
            } catch (e: Exception) {
                runOnUiThread { status.text = "Error: ${e.message}" }
            }
        }
    }

    private fun play() {
        val v = versions.getOrNull(spinner.selectedItemPosition) ?: return
        if (!File(gameDir, "versions/${v.id}/${v.id}.json").exists()) {
            status.text = "Download versi dulu"; return
        }
        saveSettings()
        startActivity(Intent(this, GameActivity::class.java).apply {
            putExtra("version", v.id)
            putExtra("gameDir", gameDir.absolutePath)
        })
    }
}
