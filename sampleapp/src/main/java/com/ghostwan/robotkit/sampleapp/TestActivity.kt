package com.ghostwan.robotkit.sampleapp

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.ghostwan.robotkit.naoqi.robot.LocalPepper
import com.ghostwan.robotkit.util.ui

class TestActivity : AppCompatActivity() {

    private var step: Int = 0
    companion object {
        const val TAG = "TestActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        val pepper = LocalPepper(this)
        ui{
            testConnection(pepper)
            testConnection(pepper)
        }
    }

    private suspend fun testConnection(pepper: LocalPepper) {
        Log.i(TAG, "Connecting...")
        pepper.connect()
        Log.i(TAG, "Connected!")
        pepper.say("step $step")
        Log.i(TAG, "Say")
        Log.i(TAG, "Disconnecting")
        pepper.disconnect()
        Log.i(TAG, "Disconnected")
        step++
    }

    private fun step() {
    }

}
