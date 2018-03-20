package com.ghostwan.robotkit.robot.pepper.`object`

import android.support.annotation.RawRes
import java.util.*

/**
 * Created by erwan on 10/03/2018.
 */
interface Concept

class ResConcept : Concept {
    internal var phrases = ArrayList<Int>()

    constructor(@RawRes vararg phrases:Int) {
        this.phrases = phrases.toList() as ArrayList<Int>
    }

    fun isPhraseInConcept(phrase: Int) = phrases.contains(phrase)
}


class StringConcept : Concept {
    internal var phrases = ArrayList<String>()

    constructor(vararg phrases:String) {
        this.phrases = phrases.toList() as ArrayList<String>
    }

    fun isPhraseInConcept(phrase: Int) = phrases.contains(phrase)
}