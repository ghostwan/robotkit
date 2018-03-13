package com.ghostwan.robotkit.robot.pepper.`object`

import android.content.Context
import com.aldebaran.qi.sdk.`object`.conversation.Discuss

/**
 * Created by erwan on 12/03/2018.
 */
class Discussion {
    var topics : MutableList<String> = ArrayList()

    var discuss: Discuss? = null

    /*
     https://regex101.com/
     (?!\$\d{1} )(\$\w+)
     Match the $name $1name but not $1
     test string val test = " u1:(my name is _*) \$1 \$2 \$22 it's a nice name ! \$name=\$1 and what's "
     The goal is to find all variable names in the topics so we can watch all variable
    */

    constructor(vararg strings: String) {
        for (string in strings) {
            topics.add(string)
        }
    }

    constructor(context: Context, vararg integers:Int) {
        for (integer in integers) {
            topics.add(context.getString(integer))
        }
    }

    fun getVariable(topicContent : String) {
        val reg = "(?!\\\$\\d+ )(\\\$\\w+)".toRegex()
        reg.findAll(topicContent)
                .map { it.groups[1]?.value?.replace("\$","") }
                .distinct()
                .forEach { println(it) }
    }

    fun save() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}