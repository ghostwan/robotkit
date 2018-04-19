package com.ghostwan.robotkit.util

import android.util.Log

/**
 * Utilities free functions
 */

const val TAG = "RobotKit"

fun infoLog(message: String, tag: String=TAG) {
    Log.i(tag, message)
}

fun warningLog(message: String, tag: String=TAG) {
    Log.w(tag, message)
}
fun errorLog(message: String, tag: String=TAG){
    Log.e(tag, message)
}

fun exceptionLog(t: Throwable?, message: String? = "error", tag: String=TAG) {
    Log.e(tag, message, t)
}