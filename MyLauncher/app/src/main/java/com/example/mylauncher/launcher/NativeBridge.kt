package com.example.mylauncher.launcher

import android.view.Surface

object NativeBridge {
    init { System.loadLibrary("launcher_native") }

    external fun setSurface(surface: Surface?)
    external fun setenv(key: String, value: String)
    external fun dlopenGlobal(path: String): Int
    external fun startJvm(jreHome: String, jvmArgs: Array<String>, mainClass: String, appArgs: Array<String>): Int
}
