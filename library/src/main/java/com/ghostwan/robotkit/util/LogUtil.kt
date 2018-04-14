package com.ghostwan.robotkit.util

import android.util.Log

/**
 * Utilities free functions
 */

const val TAG = "RobotKit"

fun info(message: String, tag: String=TAG) {
    Log.i(tag, message)
}

fun warning(message: String, tag: String=TAG) {
    Log.w(tag, message)
}

fun exception(t: Throwable?, message: String? = "error", tag: String=TAG) {
    Log.e(tag, message, t)
}