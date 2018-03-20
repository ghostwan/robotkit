package com.ghostwan.robotkit.sampleapp.scenario

import com.ghostwan.robotkit.sampleapp.ParentActivity
import com.ghostwan.robotkit.sampleapp.R

class HelloWorld : ParentActivity() {

    override fun layout() = R.layout.activity_hello_world

    override suspend fun start() {

    }

    override fun scenarioName(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
