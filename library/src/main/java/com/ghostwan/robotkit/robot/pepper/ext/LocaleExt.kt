package com.ghostwan.robotkit.robot.pepper.ext

import android.content.Context
import android.os.Build
import com.aldebaran.qi.sdk.`object`.locale.Language
import com.aldebaran.qi.sdk.`object`.locale.Region
import java.util.*


/**
 * Created by erwan on 19/03/2018.
 */
fun Locale.toNaoqiLocale() : com.aldebaran.qi.sdk.`object`.locale.Locale? {
    return when(this) {
        Locale.FRENCH ->  com.aldebaran.qi.sdk.`object`.locale.Locale(Language.FRENCH, Region.FRANCE)
        Locale.ENGLISH ->  com.aldebaran.qi.sdk.`object`.locale.Locale(Language.ENGLISH, Region.UNITED_STATES)
        Locale.JAPANESE ->  com.aldebaran.qi.sdk.`object`.locale.Locale(Language.JAPANESE, Region.JAPAN)
        Locale.CHINESE ->  com.aldebaran.qi.sdk.`object`.locale.Locale(Language.CHINESE, Region.CHINA)
        Locale.GERMAN ->  com.aldebaran.qi.sdk.`object`.locale.Locale(Language.GERMAN, Region.GERMANY)
        Locale.ITALIAN ->  com.aldebaran.qi.sdk.`object`.locale.Locale(Language.ITALIAN, Region.ITALY)
        Locale("es") ->  com.aldebaran.qi.sdk.`object`.locale.Locale(Language.SPANISH, Region.SPAIN)
        else -> null
    }
}

fun getCurrentLocale(context: Context): Locale {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.resources.configuration.locales.get(0)
    } else {
        context.resources.configuration.locale
    }
}