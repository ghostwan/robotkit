package com.ghostwan.robotkit.sampleapp.samples

import com.ghostwan.robotkit.robot.pepper.ext.inUIAsync
import com.ghostwan.robotkit.sampleapp.R
import com.ghostwan.robotkit.sampleapp.helpers.ParentActivity
import kotlinx.android.synthetic.main.activity_multi_locales.*


class HelloWorldActivity : ParentActivity() {

    override fun scenarioName(): String = "Hello World Parallel"

    override suspend fun onStartAction() {
        val t1 = inUIAsync { pepper.say(R.string.hello_world) }
        val t2 = inUIAsync { pepper.animate(R.raw.hello_anim) }
        val t3 = inUIAsync { textview.setText(R.string.hello_world) }

        t1.await()
        t2.await()
        t3.await()
    }


}
