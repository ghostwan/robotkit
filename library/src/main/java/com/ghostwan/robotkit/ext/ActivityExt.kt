package com.ghostwan.robotkit.ext

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.IBinder
import android.support.annotation.RawRes
import android.support.annotation.StringRes
import com.ghostwan.robotkit.exception.RobotUnavailableException
import com.ghostwan.robotkit.exception.ServiceDisconnectedException
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.util.*
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine


/**
 * Created by erwan on 19/03/2018.
 */
fun Context.createLocalizedContext(locale : Locale) : Context{
    var conf = resources.configuration
    conf = Configuration(conf)
    conf.setLocale(locale)
    return createConfigurationContext(conf)
}

fun Context.getLocalizedString(@StringRes resId : Int, locale : Locale?=null) : String{
    if(locale == null)
        return getString(resId)
    return createLocalizedContext(locale).resources.getString(resId)
}

fun Context.getRaw(resId: Int): String {
    val inputStream = resources.openRawResource(resId)
    return Scanner(inputStream, "UTF-8").useDelimiter("\\A").next()
}

fun Context.getLocalizedRaw(@RawRes resId : Int, locale : Locale?=null) : String{
    if(locale == null)
        return getRaw(resId)
    return createLocalizedContext(locale).getRaw(resId)
}

data class LocalService(val binder: IBinder?, val connection: ServiceConnection)

suspend fun Context.getLocalService(packageName: String, action: String, flags : Int = BIND_AUTO_CREATE): LocalService {
    return suspendCoroutine { cont: Continuation<LocalService> ->
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                cont.resume(LocalService(service, this))
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                cont.resumeWithException(ServiceDisconnectedException(name?.className))
            }

        }
        val intent = Intent(action)
        intent.`package` = packageName
        if(!bindService(intent, connection, flags)) {
            cont.resumeWithException(RobotUnavailableException("RobotService not available!"))
        }
    }
}

fun Activity.inUI(onRun: suspend Activity.() -> Unit): Job {
    return launch(UI, block = {
        onRun(this@inUI)
    })
}

fun Activity.inUIAsync(onRun: suspend Activity.() -> Unit): Deferred<Unit> {
    return async(UI, block = {
        onRun(this@inUIAsync)
    })
}

fun Activity.inUISafe(onRun: suspend Activity.() -> Unit, onError : (Throwable?) -> Unit ): Deferred<Unit> {
    val job = async (UI, block = {
        onRun(this@inUISafe)
    })
    job.invokeOnCompletion {
        onError(it)
    }
    return job
}

fun Activity.inBackground(onRun: suspend Activity.() -> Unit): Job {
    return launch(block = {
        onRun(this@inBackground)
    })
}

fun Activity.inBackgroundAsync(onRun: suspend Activity.() -> Unit): Deferred<Unit> {
    return async(block = {
        onRun(this@inBackgroundAsync)
    })
}

fun Activity.inBackgroundSafe(onRun: suspend Activity.() -> Unit, onError : (Throwable?) -> Unit): Deferred<Unit> {
    val job = async (block = {
        onRun(this@inBackgroundSafe)
    })
    job.invokeOnCompletion {
        onError(it)
    }
    return job
}

