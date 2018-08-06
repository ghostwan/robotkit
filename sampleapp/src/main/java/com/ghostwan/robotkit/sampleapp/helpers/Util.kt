package com.ghostwan.robotkit.sampleapp.helpers

import android.content.Context
import android.media.MediaPlayer
import com.ghostwan.robotkit.ext.random
import com.ghostwan.robotkit.sampleapp.R

private var mPlayer: MediaPlayer = MediaPlayer()

private val soundResources = arrayOf(
        R.raw.wait1_sound,
        R.raw.wait2_sound,
        R.raw.wait3_sound,
        R.raw.wait4_sound,
        R.raw.wait4_sound,
        R.raw.wait6_sound,
        R.raw.wait7_sound)

fun playSound(context: Context) {
    val index = (0..soundResources.size).random()
    println("Index  : $index / ${soundResources.size}")
    mPlayer = MediaPlayer.create(context, soundResources[index])
    mPlayer.setOnCompletionListener {
        mPlayer.release()
        playSound(context)
    }
    mPlayer.start()
}

fun stopSound() {
    mPlayer.stop()
    mPlayer.release()
}