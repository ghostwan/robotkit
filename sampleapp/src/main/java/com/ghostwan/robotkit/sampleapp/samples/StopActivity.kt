package com.ghostwan.robotkit.sampleapp.samples

import android.content.res.Resources
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.aldebaran.qi.QiException
import com.ghostwan.robotkit.`object`.Action
import com.ghostwan.robotkit.`object`.Failure
import com.ghostwan.robotkit.`object`.Result
import com.ghostwan.robotkit.`object`.Success
import com.ghostwan.robotkit.exception.RobotUnavailableException
import com.ghostwan.robotkit.ext.inUISafe
import com.ghostwan.robotkit.ext.setOnClickCoroutine
import com.ghostwan.robotkit.ext.setOnClickSafeCoroutine
import com.ghostwan.robotkit.naoqi.`object`.ResConcept
import com.ghostwan.robotkit.naoqi.robot.LocalPepper
import com.ghostwan.robotkit.naoqi.robot.Pepper
import com.ghostwan.robotkit.sampleapp.R
import com.ghostwan.robotkit.util.exceptionLog
import kotlinx.android.synthetic.main.activity_stop_acivity.*
import kotlinx.coroutines.CancellationException

class StopActivity : AppCompatActivity() {

    private val pepper by lazy {
        if (intent.hasExtra("address")) {
            Pepper(this, intent.getStringExtra("address"), "nao")
        } else {
            LocalPepper(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stop_acivity)

        buttonSay.setOnClickCoroutine{
            pepper.say(R.string.lorem_lpsum, onResult = {onResult(it)})
        }

        buttonAnimate.setOnClickCoroutine {
            pepper.animate(R.raw.taichichuan_anim) {onResult(it)}
        }

        buttonDiscuss.setOnClickSafeCoroutine ({
            pepper.discuss(R.raw.presentation_discussion, gotoBookmark = "intro", onResult = {onResult(it)})
        }, onError = this::onError )

        buttonSayAnimate.setOnClickCoroutine {
            pepper.say(R.string.lorem_lpsum, R.raw.taichichuan_anim, onResult = {onResult(it)})
        }
        buttonListen.setOnClickCoroutine {
            val helloConcept = ResConcept(R.string.hello, R.string.hi)
            pepper.listen(helloConcept, onResult = {
                when(it) {
                    is Success -> displayInfo("Listen succeed")
                    is Failure -> onError(it.exception)
                }
            })
        }

        buttonStopTalking.setOnClickCoroutine {
            pepper.stop(Action.TALKING)
        }
        buttonStopListening.setOnClickCoroutine {
            pepper.stop(Action.LISTENING)
        }
        buttonStopMoving.setOnClickCoroutine {
            pepper.stop(Action.MOVING)
        }
    }


    override fun onStart() {
        super.onStart()
        inUISafe({
            actionsLayout.visibility = View.INVISIBLE
            stopActionsLayout.visibility = View.INVISIBLE
            pepper.connect()
            actionsLayout.visibility = View.VISIBLE
            stopActionsLayout.visibility = View.VISIBLE
        }, this::onError)
    }

    override fun onStop() {
        super.onStop()
        inUISafe({
            pepper.disconnect()
            actionsLayout.visibility = View.INVISIBLE
            stopActionsLayout.visibility = View.INVISIBLE
        }, this::onError)
    }

    fun <T> onResult(result: Result<T>) {
        if(result is Failure)
            onError(result.exception)
    }

    fun onError(throwable: Throwable?) {
        val message = when (throwable) {
            is QiException -> "Robot Exception ${throwable.message}"
            is RobotUnavailableException -> "Robot unavailable ${throwable.message}"
            is Resources.NotFoundException -> "Android resource missing ${throwable.message}"
            is CancellationException -> "Execution was stopped"
            else -> throwable?.message
        }
        if(throwable !is CancellationException && throwable != null)
            exceptionLog(throwable ,"onError")
        message?.let { displayInfo(message) }
    }

    fun displayInfo(string: String, duration: Int = Snackbar.LENGTH_LONG) {
        Snackbar.make(stopActionsLayout, string, duration).show()
    }
}
