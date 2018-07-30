package com.ghostwan.robotkit.sampleapp.samples

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import com.ghostwan.robotkit.naoqi.robot.LocalPepper
import com.ghostwan.robotkit.naoqi.robot.Pepper
import com.ghostwan.robotkit.sampleapp.R
import com.ghostwan.robotkit.util.ui

class TestConnectionActivity : AppCompatActivity() {

    private val pepper by lazy {
        if (intent.hasExtra("address")) {
            Pepper(this, intent.getStringExtra("address"), "nao")
        } else {
            LocalPepper(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_connection)
    }

    fun onConnect(view: View) {
        val button = (view as Button)
        if(button.text == getText(R.string.connect)) {
            button.setText(R.string.disconnect)
            ui {
                pepper.connect()
                pepper.say("I'm connected!")
            }
        }
        else {
            button.setText(R.string.connect)
            ui {
                pepper.say("Disconnection...")
                pepper.disconnect()
            }
        }
    }
}
