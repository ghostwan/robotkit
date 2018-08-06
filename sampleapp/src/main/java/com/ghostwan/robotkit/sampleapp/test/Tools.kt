package com.ghostwan.robotkit.sampleapp.test

import android.content.res.Resources
import android.util.Log
import android.widget.TextView
import com.aldebaran.qi.QiException
import com.ghostwan.robotkit.exception.RobotUnavailableException
import com.ghostwan.robotkit.util.errorLog
import com.ghostwan.robotkit.util.exceptionLog
import kotlinx.coroutines.experimental.CancellationException


internal var step: Int = 0

internal fun log(text: String, testTitle: TextView?=null) {
    step++
    val message = "$step) $text"
    Log.i(TestActivity.TAG, message)
    testTitle?.text = message
}

internal fun start() {
    log("------------ START ------------")
}

internal fun end() {
    log("------------ END ------------")
}

internal class Tools {
    companion object {
        internal fun onError(throwable: Throwable?) {
            val message = when (throwable) {
                is QiException -> "Robot Exception ${throwable.message}"
                is RobotUnavailableException -> "Robot unavailable ${throwable.message}"
                is Resources.NotFoundException -> "Android resource missing ${throwable.message}"
                is CancellationException -> "Execution was stopped"
                else -> throwable?.message
            }
            if (throwable !is CancellationException && throwable != null)
                exceptionLog(throwable, "onError")
            else
                message?.let { errorLog(it) }
        }
    }

}

