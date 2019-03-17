package com.ghostwan.robotkit.sampleapp.samples

import android.media.MediaPlayer
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.aldebaran.qi.Promise
import com.ghostwan.robotkit.`object`.Action
import com.ghostwan.robotkit.`object`.Cancel
import com.ghostwan.robotkit.`object`.Failure
import com.ghostwan.robotkit.`object`.Success
import com.ghostwan.robotkit.ext.inUI
import com.ghostwan.robotkit.ext.setOnClickCoroutine
import com.ghostwan.robotkit.ext.startAndAwait
import com.ghostwan.robotkit.ext.stopAndRelease
import com.ghostwan.robotkit.naoqi.`object`.Discussion
import com.ghostwan.robotkit.naoqi.robot.LocalPepper
import com.ghostwan.robotkit.sampleapp.R
import kotlinx.android.synthetic.main.activity_test_executor.*

class DiscussAndExecutorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_executor)
        val pepper = LocalPepper(this)
        inUI {
            startButton.isEnabled = false
            pepper.connect()
            val discussion = Discussion(this, R.raw.test_execute)
            discussion.setExecutor(pepper, "animateSync", onStopExecute = { pepper.stop(Action.MOVING) }) {
                pepper.animate(R.raw.taichichuan_a001)
            }
            discussion.setAsyncExecutor(pepper, "animateAsync", onStopExecute = { pepper.stop(Action.MOVING) }) {
                val promise = Promise<Void>()
                pepper.animate(R.raw.taichichuan_a001, onResult = {
                    when (it) {
                        is Success -> promise.setValue(null)
                        is Failure -> promise.setError(it.exception.message)
                        is Cancel -> promise.setCancelled()
                    }
                })
                promise.future
            }
            var mediaPlayer = MediaPlayer()
            discussion.setAsyncExecutor(pepper, "playAsync", onExecute = {
                mediaPlayer = MediaPlayer.create(this, R.raw.music)
                val promise = Promise<Void>()
                mediaPlayer.setOnCompletionListener {
                    promise.setValue(null)
                    mediaPlayer.release()
                }
                mediaPlayer.setOnErrorListener { _, what, _ ->
                    promise.setError("Error : $what")
                    true
                }
                mediaPlayer.start()
                promise.future
            }, onStopExecute = {
                mediaPlayer.stop()
                mediaPlayer.release()
            })

            discussion.setExecutor(pepper, "playSync", onStopExecute = { mediaPlayer.stopAndRelease() }) {
                mediaPlayer = MediaPlayer.create(this, R.raw.music)
                mediaPlayer.startAndAwait()
            }

            startButton.isEnabled = true
            startButton.text = "Start"
            startButton.setOnClickCoroutine {
                if (startButton.text == "Start") {
                    startButton.text = "Stop"
                    pepper.discuss(discussion)
                } else {
                    pepper.stop(Action.DISCUSSING)
                    startButton.text = "Start"
                }
            }
        }

    }
}
