package com.ghostwan.robotkit.sampleapp.samples

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import com.ghostwan.robotkit.ext.setOnClickCoroutine
import com.ghostwan.robotkit.naoqi.robot.LocalPepper
import com.ghostwan.robotkit.sampleapp.R

import kotlinx.android.synthetic.main.activity_basic_hello_world.*

class BasicHelloWorldActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basic_hello_world)
        setSupportActionBar(toolbar)

        val pepper = LocalPepper(this)

        fab.setOnClickCoroutine {
            if (pepper.isConnected()) {
                pepper.say(R.string.bye, R.raw.bye_anim)
                pepper.disconnect()
                textView.text = "Robot Disconnected"
            } else {
                pepper.connect()
                pepper.say(R.string.hello_world, onStart = {
                    pepper.animate(R.raw.hello_anim)
                    textView.text = "Robot Connected"
                    Snackbar.make(this, R.string.hello_world, Snackbar.LENGTH_LONG).show()
                })
            }
        }
    }

}
