package com.ghostwan.robotkit.ext

import android.view.View
import com.ghostwan.robotkit.util.ui
import com.ghostwan.robotkit.util.uiSafe

fun View.setOnClickCoroutine(onRun: suspend View.() -> Unit) {
    setOnClickListener {
        ui({
            onRun(this@setOnClickCoroutine)
        })
    }
}

fun View.setOnClickSafeCoroutine(onRun: suspend View.() -> Unit, onError: (Throwable?) -> Unit) {
    setOnClickListener {
        uiSafe({
            onRun(this@setOnClickSafeCoroutine)
        }, onError)
    }
}

