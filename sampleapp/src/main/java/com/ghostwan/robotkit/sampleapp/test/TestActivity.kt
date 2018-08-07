package com.ghostwan.robotkit.sampleapp.test

import android.media.MediaPlayer
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.aldebaran.qi.Promise
import com.ghostwan.robotkit.ext.setOnClickSafeCoroutine
import com.ghostwan.robotkit.naoqi.robot.LocalPepper
import com.ghostwan.robotkit.naoqi.robot.Pepper
import com.ghostwan.robotkit.naoqi.robot.isOnLocalPepper
import com.ghostwan.robotkit.sampleapp.R
import kotlinx.android.synthetic.main.activity_test.*

class TestActivity : AppCompatActivity() {

    companion object {
        const val TAG = "TestActivity"
    }

    val pepper = if (isOnLocalPepper()) {
        LocalPepper(this)
    } else {
        Pepper(this, intent.getStringExtra("address"), "nao")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        testButton.setOnClickSafeCoroutine({
            startTest()
        },  Tools.Companion::onError)
    }

    private suspend fun startTest() {
        testExecuteQiSDK(pepper, this@TestActivity)
    }


    private suspend fun testConnectionRobotKit(pepper: Pepper) {
        log("Connecting...")
        pepper.connect()
        Log.i(TAG, "Disconnecting")
        pepper.disconnect()
        Log.i(TAG, "Disconnected")
    }




    private fun execute() {
        val promise = Promise<Boolean>()
        val mediaPlayer = MediaPlayer()
        mediaPlayer.setOnPreparedListener {
            promise.setValue(true)
        }
        mediaPlayer.setOnErrorListener { mp, what, extra ->
            promise.setError("Error code $what")
            true
        }
        mediaPlayer.start()
        promise.setOnCancel {
            mediaPlayer.stop()
        }
        promise.future.get()
    }



}
