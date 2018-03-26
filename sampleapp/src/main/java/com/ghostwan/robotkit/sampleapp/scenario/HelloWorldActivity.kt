package com.ghostwan.robotkit.sampleapp.scenario

import com.ghostwan.robotkit.robot.pepper.util.uiAsync
import com.ghostwan.robotkit.sampleapp.ParentActivity
import com.ghostwan.robotkit.sampleapp.R
import kotlinx.android.synthetic.main.activity_multi_locales.*


class HelloWorldActivity : ParentActivity() {

    override fun scenarioName(): String = "Hello World Parallel"

    override suspend fun onStartAction() {
        val t1 = uiAsync { pepper.say(R.string.hello_world) }
        val t2 = uiAsync { pepper.animate(R.raw.hello_anim) }
        val t3 = uiAsync { textview.setText(R.string.hello_world) }

        t1.await()
        t2.await()
        t3.await()
    }


}
