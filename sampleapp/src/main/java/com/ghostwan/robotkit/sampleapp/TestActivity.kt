package com.ghostwan.robotkit.sampleapp

import android.media.MediaPlayer
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class TestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
    }

    private lateinit var mPlayer: MediaPlayer

    fun onStart(view: View) {
        mPlayer = MediaPlayer.create(this, R.raw.intero1)
        mPlayer.isLooping = true
        mPlayer.start()

    }

    fun onStop(view: View) {
        mPlayer.stop()
    }
}
