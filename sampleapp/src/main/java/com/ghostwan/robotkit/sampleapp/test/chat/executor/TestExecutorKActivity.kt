package com.ghostwan.robotkit.sampleapp.test.chat.executor

import android.media.MediaPlayer
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.aldebaran.qi.Promise
import com.ghostwan.robotkit.`object`.Action
import com.ghostwan.robotkit.`object`.Failure
import com.ghostwan.robotkit.`object`.Success
import com.ghostwan.robotkit.ext.inUI
import com.ghostwan.robotkit.ext.startAndAwait
import com.ghostwan.robotkit.ext.stopAndRelease
import com.ghostwan.robotkit.naoqi.`object`.Discussion
import com.ghostwan.robotkit.naoqi.robot.LocalPepper
import com.ghostwan.robotkit.sampleapp.R

class TestExecutorKActivity : AppCompatActivity() {

    companion object {
        private val TAG = "TestExecutorKActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_executor)
        val pepper = LocalPepper(this)
        inUI {
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
                    }
                })
                promise.future
            }
            val mediaPlayer = MediaPlayer.create(this, R.raw.wait6_sound)
            discussion.setAsyncExecutor(pepper, "playAsync", onExecute = {
                val promise = Promise<Void>()
                mediaPlayer.setOnCompletionListener {
                    promise.setValue(null)
                    mediaPlayer.release()
                }
                mediaPlayer.setOnErrorListener { _, what, _ ->
                    promise.setError("Error : $what")
                    true
                }
                promise.future
            }, onStopExecute = {
                mediaPlayer.stop()
                mediaPlayer.release()
            })

            discussion.setExecutor(pepper, "playSync", onStopExecute = { mediaPlayer.stopAndRelease() }) {
                mediaPlayer.startAndAwait()
            }
        }

    }
}
