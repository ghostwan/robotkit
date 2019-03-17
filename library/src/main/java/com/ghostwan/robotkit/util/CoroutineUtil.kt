package com.ghostwan.robotkit.util

import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main


/**
 * Call a coroutine lambda in Android UI thread
 */
fun ui(onRun: suspend CoroutineScope.() -> Unit): Job {
    return GlobalScope.launch(Main, block = onRun)
}

fun uiAsync(onRun: suspend CoroutineScope.() -> Unit): Deferred<Unit> {
    return GlobalScope.async(Main, block = onRun)
}

//TODO expose Activity instead of CoroutineScope
fun uiSafe(onRun: suspend CoroutineScope.() -> Unit, onError : (Throwable?) -> Unit ): Deferred<Unit> {
    val job = GlobalScope.async (Main, block = onRun)
    job.invokeOnCompletion {
        onError(it)
    }
    return job
}

fun background(onRun: suspend CoroutineScope.() -> Unit): Job {
    return GlobalScope.launch(block = onRun)
}

fun backgroundAsync(onRun: suspend CoroutineScope.() -> Unit): Deferred<Unit> {
    return GlobalScope.async(block = onRun)
}

fun backgroundSafe(onRun: suspend CoroutineScope.() -> Unit, onError : (Throwable?) -> Unit): Deferred<Unit> {
    val job = GlobalScope.async (block = onRun)
    job.invokeOnCompletion {
        onError(it)
    }
    return job
}


