package com.ghostwan.robotkit.sampleapp.samples

import android.content.res.Resources
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.aldebaran.qi.QiException
import com.ghostwan.robotkit.robot.pepper.MyPepper
import com.ghostwan.robotkit.robot.pepper.`object`.*
import com.ghostwan.robotkit.robot.pepper.exception.RobotUnavailableException
import com.ghostwan.robotkit.robot.pepper.ext.inUISafe
import com.ghostwan.robotkit.robot.pepper.ext.setOnClickCoroutine
import com.ghostwan.robotkit.robot.pepper.util.exception
import com.ghostwan.robotkit.sampleapp.R
import kotlinx.android.synthetic.main.activity_stop_acivity.*
import kotlinx.coroutines.experimental.CancellationException

class StopActivity : AppCompatActivity() {

    private lateinit var pepper: MyPepper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stop_acivity)
        pepper = MyPepper(this)

        buttonSay.setOnClickCoroutine{
            pepper.say(R.string.lorem_lpsum, onResult = this@StopActivity::onResult)
        }
        buttonAnimate.setOnClickCoroutine {
            pepper.animate(R.raw.taichichuan_anim, onResult = this@StopActivity::onResult)
        }
        buttonDiscuss.setOnClickCoroutine {
            pepper.discuss(R.raw.presentation_discussion, gotoBookmark = "intro", onResult = this@StopActivity::onResult)
        }
        buttonSayAnimate.setOnClickCoroutine {
            pepper.say(R.string.lorem_lpsum, R.raw.taichichuan_anim, onResult = this@StopActivity::onResult)
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
            exception(throwable ,"onError")
        message?.let { displayInfo(message) }
    }

    fun displayInfo(string: String, duration: Int = Snackbar.LENGTH_LONG) {
        Snackbar.make(stopActionsLayout, string, duration).show()
    }
}
