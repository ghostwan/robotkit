package com.ghostwan.robotkit.sampleapp.scenario

import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import com.ghostwan.robotkit.robot.pepper.ext.getCurrentLocale
import com.ghostwan.robotkit.robot.pepper.ext.setText
import com.ghostwan.robotkit.sampleapp.ParentActivity
import com.ghostwan.robotkit.sampleapp.R
import kotlinx.android.synthetic.main.activity_hello_world.*
import java.util.*


class HelloWorld : ParentActivity() {

    lateinit var locale : Locale

    override fun layout(): Int {
        return R.layout.activity_hello_world
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locale = getCurrentLocale(this)
    }

    override suspend fun start() {
        pepper.say(R.string.hello_world, locale = locale)
        textview.setText(R.string.hello_world, locale)
        pepper.animate(R.raw.hello_anim)
    }

    override fun scenarioName(): String {
        return "Hello World"
    }

    fun onRadioButtonClicked(view: View) {
        // Is the button now checked?
        val checked = (view as RadioButton).isChecked

        // Check which radio button was clicked
        when (view.getId()) {
            R.id.radio_english -> if (checked)  {
                locale = Locale.ENGLISH
            }
            R.id.radio_french -> if (checked) {
                locale = Locale.FRENCH
            }
        }
    }

}
