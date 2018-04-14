package com.ghostwan.robotkit.sampleapp.samples

import com.ghostwan.robotkit.ext.setText
import com.ghostwan.robotkit.sampleapp.helpers.MultiLocaleActivity
import com.ghostwan.robotkit.sampleapp.R
import kotlinx.android.synthetic.main.activity_multi_locales.*


class HelloWorldMultiLocaleActivity : MultiLocaleActivity() {

    override fun scenarioName(): String = "Hello World Multi locales"

    override suspend fun onStartAction() {
        pepper.say(R.string.hello_world, locale = locale)
        textview.setText(R.string.hello_world, locale = locale)
        pepper.animate(R.raw.hello_anim)
    }


}
