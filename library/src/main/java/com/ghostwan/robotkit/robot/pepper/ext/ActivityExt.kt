package com.ghostwan.robotkit.robot.pepper.ext

import android.content.Context
import android.content.res.Configuration
import android.support.annotation.RawRes
import android.support.annotation.StringRes
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
