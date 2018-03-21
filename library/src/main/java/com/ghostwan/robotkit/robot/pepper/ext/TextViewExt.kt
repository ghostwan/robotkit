package com.ghostwan.robotkit.robot.pepper.ext

import android.content.res.Configuration
import android.support.annotation.StringRes
import android.widget.TextView
import java.util.*

/**
 * Created by erwan on 22/03/2018.
 */
fun TextView.setText(@StringRes resId : Int, locale : Locale){
    var conf = resources.configuration
    conf = Configuration(conf)
    conf.setLocale(locale)
    val localizedContext = context.createConfigurationContext(conf)
    text = localizedContext.resources.getString(resId)
}