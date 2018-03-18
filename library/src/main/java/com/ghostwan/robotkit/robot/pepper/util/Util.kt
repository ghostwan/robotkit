package com.ghostwan.robotkit.robot.pepper.util

import android.util.Log
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import java.lang.ref.WeakReference
import kotlin.reflect.KProperty

/**
 * Created by erwan on 18/03/2018.
 */

const val TAG = "RobotKit"


fun ui(onRun: suspend CoroutineScope.() -> Unit): Job {
    return launch(UI, block = onRun)
}

fun uiAsync(onRun: suspend CoroutineScope.() -> Unit): Deferred<Unit> {
    return async(UI, block = onRun)
}

fun uiSafe(onRun: suspend CoroutineScope.() -> Unit, onError : (Throwable?) -> Unit ): Deferred<Unit> {
    val job = async (UI, block = onRun)
    job.invokeOnCompletion {
        onError.invoke(it)
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
        onError.invoke(it)
    }
    return job
}

/**
 * Experimental delegate to have weak properties without boilerplate.
 *
 * See [weakRef] for usage.
 */
class WeakRefHolder<T>(private var _value: WeakReference<T>) {

    operator fun getValue(thisRef: Any, property: KProperty<*>): T {
        return _value.get()!!
    }

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        _value = WeakReference(value)
    }
}

/**
 * Use the `by` keyword to delegate weak references.
 *
 * Internally this creates a [WeakRefHolder] that will store the real
 * [WeakReference]. Example usage:
 *
 *     var weakContext: Context? by weakRef(null)
 *     …
 *     weakContext = strongContext
 *     …
 *     context = weakContext
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <T> weakRef(value: T) = WeakRefHolder<T>(WeakReference(value))


fun info(message: String, tag: String=TAG) {
    Log.i(tag, message)
}

fun warning(message: String, tag: String=TAG) {
    Log.w(tag, message)
}

fun exception(t: Throwable?, message: String? = "error", tag: String=TAG) {
    Log.e(tag, message, t)
}