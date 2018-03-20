package com.ghostwan.robotkit.robot.pepper.`object`

import android.support.annotation.StringRes
import java.util.*

/**
 * Created by erwan on 10/03/2018.
 */
sealed class Concept

class ResConcept : Concept {
    internal var phrases = ArrayList<Int>()

    constructor(@StringRes vararg phrases:Int) {
        this.phrases = phrases.toList() as ArrayList<Int>
    }

    fun isPhraseInConcept(phrase: Int) = phrases.contains(phrase)
}


class StrConcept : Concept {
    internal var phrases = ArrayList<String>()

    constructor(vararg phrases:String) {
        this.phrases = phrases.toList() as ArrayList<String>
    }

    fun isPhraseInConcept(phrase: String) = phrases.contains(phrase)
}