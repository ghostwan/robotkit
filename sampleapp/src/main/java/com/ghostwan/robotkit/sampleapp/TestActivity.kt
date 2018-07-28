package com.ghostwan.robotkit.sampleapp

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.ghostwan.robotkit.naoqi.robot.LocalPepper
import com.ghostwan.robotkit.util.ui
import kotlinx.android.synthetic.main.activity_test.*

class TestActivity : AppCompatActivity() {

    private var step: Int = 0
    companion object {
        const val TAG = "TestActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        val pepper = LocalPepper(this)
        var step: Int = 0
        ui{
            log("test connection")
            testConnection(pepper)
            log("test connection")
            testConnection(pepper)

            log("tests")
            pepper.connect()


            log("animate")
            pepper.animate(R.raw.bye_anim)
            log("say")
            pepper.say("say ${++step} ")
            log("say")
            pepper.say("say ${++step} ")
            log("discuss")
            pepper.discuss(R.raw.test_topic, gotoBookmark = "start")
            log("say and animate")
            pepper.say(R.string.hello_world, R.raw.hello_anim)


            log("disconnect")
            pepper.disconnect()
            finish()
        }
    }

    private suspend fun testConnection(pepper: LocalPepper) {
        log("Connecting...")
        pepper.connect()

        Log.i(TAG, "Disconnecting")
        pepper.disconnect()
        Log.i(TAG, "Disconnected")
    }

    private fun log(text: String) {
        step++
        val message = "$step) $text"
        Log.i(TAG, message)
        testTitle.text = message
    }

}
