package com.ghostwan.robotkit.robot.pepper.`object`

import android.content.Context
import com.aldebaran.qi.sdk.`object`.conversation.Phrase

/**
 * Created by erwan on 10/03/2018.
 */
class Concept{
    internal var phrases : MutableList<Phrase> = ArrayList()

    constructor(vararg strings: String) {
        for (string in strings) {
            phrases.add(Phrase(string))
        }
    }

    constructor(context: Context, vararg integers:Int) {
        for (integer in integers) {
            phrases.add(Phrase(context.getString(integer)))
        }
    }

    fun isPhraseInConcept(phrase: Phrase) = phrases.contains(phrase)

}