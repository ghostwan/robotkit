package com.ghostwan.robotkit.robot.pepper.`object`

import android.content.Context

/**
 * Created by erwan on 12/03/2018.
 */
class Discussion {
    private var topics : MutableList<String> = ArrayList()

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
}