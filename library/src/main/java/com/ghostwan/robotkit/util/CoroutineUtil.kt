package com.ghostwan.robotkit.util

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI


/**
 * Call a coroutine lambda in Android UI thread
 */
fun ui(onRun: suspend CoroutineScope.() -> Unit): Job {
    return launch(UI, block = onRun)
}

fun uiAsync(onRun: suspend CoroutineScope.() -> Unit): Deferred<Unit> {
    return async(UI, block = onRun)
}

//TODO expose Activity instead of CoroutineScope
fun uiSafe(onRun: suspend CoroutineScope.() -> Unit, onError : (Throwable?) -> Unit ): Deferred<Unit> {
    val job = async (UI, block = onRun)
    job.invokeOnCompletion {
        onError(it)
    }
    return job
}

fun background(onRun: suspend CoroutineScope.() -> Unit): Job {
    return launch(block = onRun)
}

fun backgroundAsync(onRun: suspend CoroutineScope.() -> Unit): Deferred<Unit> {
    return async(block = onRun)
}

fun backgroundSafe(onRun: suspend CoroutineScope.() -> Unit, onError : (Throwable?) -> Unit): Deferred<Unit> {
    val job = async (block = onRun)
    job.invokeOnCompletion {
        onError(it)
    }
    return job
}


