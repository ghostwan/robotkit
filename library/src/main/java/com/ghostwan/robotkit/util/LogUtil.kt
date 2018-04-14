package com.ghostwan.robotkit.util

import android.util.Log


fun info(message: String, tag: String=TAG) {
    Log.i(tag, message)
}

fun warning(message: String, tag: String=TAG) {
    Log.w(tag, message)
}

fun exception(t: Throwable?, message: String? = "error", tag: String=TAG) {
    Log.e(tag, message, t)
}