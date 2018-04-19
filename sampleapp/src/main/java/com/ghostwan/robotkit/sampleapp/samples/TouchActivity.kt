package com.ghostwan.robotkit.sampleapp.samples

import android.content.res.Resources
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.aldebaran.qi.QiException
import com.ghostwan.robotkit.exception.RobotUnavailableException
import com.ghostwan.robotkit.ext.inUI
import com.ghostwan.robotkit.ext.inUISafe
import com.ghostwan.robotkit.naoqi.`object`.BodyPart
import com.ghostwan.robotkit.naoqi.`object`.TouchState
import com.ghostwan.robotkit.naoqi.robot.LocalPepper
import com.ghostwan.robotkit.naoqi.robot.Pepper
import com.ghostwan.robotkit.sampleapp.R
import com.ghostwan.robotkit.util.exceptionLog
import kotlinx.android.synthetic.main.activity_touch.*
import kotlinx.coroutines.experimental.CancellationException

class TouchActivity : AppCompatActivity() {

    private lateinit var pepper: Pepper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inUI {
            setContentView(R.layout.activity_touch)
            pepper = if (intent.hasExtra("address")) {
                Pepper(this, intent.getStringExtra("address"))
            } else {
                LocalPepper(this)
            }
            pepper.setOnBodyTouched { bodyPart, touchState ->
                when (bodyPart) {
                    BodyPart.HEAD -> changePartVisibility(headImage, touchState)
                    BodyPart.RIGHT_HAND -> changePartVisibility(leftHandImage, touchState)
                    BodyPart.LEFT_HAND -> changePartVisibility(rightHandImage, touchState)
                    BodyPart.LEFT_BUMPER -> changePartVisibility(rightBumperImage, touchState)
                    BodyPart.RIGHT_BUMPER -> changePartVisibility(leftBumperImage, touchState)
                    BodyPart.BACK_BUMPER -> changePartVisibility(backBumperImage, touchState)
                }
            }
        }
    }

    private fun changePartVisibility(view: View, touchState: TouchState) {
        if (touchState == TouchState.TOUCHED) {
            view.visibility = View.VISIBLE
        } else {
            view.visibility = View.INVISIBLE
        }

    }

    override fun onStart() {
        super.onStart()
        inUISafe({
            if (!pepper.isConnected()) {
                pepper.connect()
            }
            if (!pepper.hasFocus()) {
                pepper.takeFocus()
            }
            displayInfo("Robot Connected")
        }, this::onError)
    }

    override fun onStop() {
        super.onStop()
        inUISafe({
            pepper.releaseFocus()
        }, this::onError)
    }

    fun onError(throwable: Throwable?) {
        val message = when (throwable) {
            is QiException -> "Robot Exception ${throwable.message}"
            is RobotUnavailableException -> "Robot unavailable ${throwable.message}"
            is Resources.NotFoundException -> "Android resource missing ${throwable.message}"
            is CancellationException -> "Execution was stopped"
            else -> throwable?.message
        }
        if (throwable !is CancellationException && throwable != null)
            exceptionLog(throwable, "onError")
        message?.let { displayInfo(message) }
    }

    fun displayInfo(string: String, duration: Int = Snackbar.LENGTH_LONG) {
        Snackbar.make(layout, string, duration).show()
    }
}
