package com.ghostwan.robotkit.naoqi.`object`

import android.support.annotation.StringRes
import java.util.*

/**
 * A Concept is a set of phrase that has similar meaning
 */
sealed class Concept

/**
 * A ResConcept is a Concept specialisation which use Android resource ID
 */
class ResConcept : Concept {
    internal var phrases = ArrayList<Int>()

    constructor(@StringRes vararg phrases:Int) {
        this.phrases = phrases.toList() as ArrayList<Int>
    }

    fun isPhraseInConcept(phrase: Int) = phrases.contains(phrase)
}

/**
 * A StrConcept is a Concept specialisation which use strings
 */
class StrConcept : Concept {
    internal var phrases = ArrayList<String>()

    constructor(vararg phrases:String) {
        this.phrases = phrases.toList() as ArrayList<String>
    }

    fun isPhraseInConcept(phrase: String) = phrases.contains(phrase)
}