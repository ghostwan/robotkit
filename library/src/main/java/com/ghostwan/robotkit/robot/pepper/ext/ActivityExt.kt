package com.ghostwan.robotkit.robot.pepper.ext

import android.content.Context
import android.content.res.Configuration
import android.support.annotation.StringRes
import java.util.*


/**
 * Created by erwan on 19/03/2018.
 */
fun Context.getLocalizedString(@StringRes resId : Int, locale : Locale) : String{
    var conf = resources.configuration
    conf = Configuration(conf)
    conf.setLocale(locale)
    val localizedContext = createConfigurationContext(conf)
    return localizedContext.resources.getString(resId)
}
