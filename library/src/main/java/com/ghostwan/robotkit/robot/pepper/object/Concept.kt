package com.ghostwan.robotkit.robot.pepper.`object`

import android.content.Context
import com.ghostwan.robotkit.robot.pepper.ext.getLocalizedString
import java.util.*

/**
 * Created by erwan on 10/03/2018.
 */
class Concept{
    internal var phrases : MutableList<String> = ArrayList()

    constructor(vararg strings: String) {
        for (string in strings) {
            phrases.add(string)
        }
    }

    constructor(context: Context, vararg integers:Int, locale : Locale?=null) {
        for (integer in integers) {
            if(locale != null)
                phrases.add(context.getLocalizedString(integer, locale))
            else
                phrases.add(context.getString(integer))
        }
    }

    fun isPhraseInConcept(phrase: String) = phrases.contains(phrase)

}