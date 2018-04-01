package com.ghostwan.robotkit.robot.pepper.ext

import android.view.View
import com.ghostwan.robotkit.robot.pepper.util.ui
import com.ghostwan.robotkit.robot.pepper.util.uiSafe

fun View.setOnClickCoroutine(onRun: suspend View.() -> Unit) {
    setOnClickListener {
        ui({
            onRun.invoke(this@setOnClickCoroutine)
        })
    }
}

fun View.setOnClickSafeCoroutine(onRun: suspend View.() -> Unit, onError: (Throwable?) -> Unit) {
    setOnClickListener {
        uiSafe({
            onRun.invoke(this@setOnClickSafeCoroutine)
        }, onError)
    }
}

