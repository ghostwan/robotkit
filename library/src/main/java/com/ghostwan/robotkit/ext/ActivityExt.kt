package com.ghostwan.robotkit.naoqi.ext

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.support.annotation.RawRes
import android.support.annotation.StringRes
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import java.util.*


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

fun Activity.inUI(onRun: suspend Activity.() -> Unit): Job {
    return launch(UI, block = {
        onRun.invoke(this@inUI)
    })
}

fun Activity.inUIAsync(onRun: suspend Activity.() -> Unit): Deferred<Unit> {
    return async(UI, block = {
        onRun.invoke(this@inUIAsync)
    })
}

fun Activity.inUISafe(onRun: suspend Activity.() -> Unit, onError : (Throwable?) -> Unit ): Deferred<Unit> {
    val job = async (UI, block = {
        onRun.invoke(this@inUISafe)
    })
    job.invokeOnCompletion {
        onError.invoke(it)
    }
    return job
}

fun Activity.inBackground(onRun: suspend Activity.() -> Unit): Job {
    return launch(block = {
        onRun.invoke(this@inBackground)
    })
}

fun Activity.inBackgroundAsync(onRun: suspend Activity.() -> Unit): Deferred<Unit> {
    return async(block = {
        onRun.invoke(this@inBackgroundAsync)
    })
}

fun Activity.inBackgroundSafe(onRun: suspend Activity.() -> Unit, onError : (Throwable?) -> Unit): Deferred<Unit> {
    val job = async (block = {
        onRun.invoke(this@inBackgroundSafe)
    })
    job.invokeOnCompletion {
        onError.invoke(it)
    }
    return job
}

