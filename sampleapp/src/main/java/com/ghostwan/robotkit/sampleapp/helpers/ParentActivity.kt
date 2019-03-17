package com.ghostwan.robotkit.sampleapp.helpers

import android.content.res.Resources
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.aldebaran.qi.QiException
import com.ghostwan.robotkit.exception.RobotUnavailableException
import com.ghostwan.robotkit.ext.inUI
import com.ghostwan.robotkit.ext.inUISafe
import com.ghostwan.robotkit.ext.setOnClickSafeCoroutine
import com.ghostwan.robotkit.naoqi.robot.LocalPepper
import com.ghostwan.robotkit.naoqi.robot.Pepper
import com.ghostwan.robotkit.sampleapp.R
import com.ghostwan.robotkit.util.exceptionLog
import kotlinx.android.synthetic.main.activity_parent.*
import kotlinx.coroutines.CancellationException

/**
 * Created by erwan on 20/03/2018.
 */
abstract class ParentActivity : AppCompatActivity() {

    companion object {
        const val START = "startAction"
        const val STOP = "stopAction"
    }

    protected val pepper by lazy {
        if (intent.hasExtra("address")) {
            Pepper(this, intent.getStringExtra("address"), "nao")
        } else {
            LocalPepper(this)
        }
    }

    protected open fun defaultLayout(): Int {
        return R.layout.activity_parent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(defaultLayout())

        title = scenarioName()
        fab.setOnClickSafeCoroutine({
            when (fab.tag) {
                START -> {
                    Snackbar.make(this, "Start ${scenarioName()}() scenario", Snackbar.LENGTH_LONG).show()
                    setFabTag(STOP)
                    try {
                        onStartAction()
                    } finally {
                        setFabTag(START)
                        textview.setText(R.string.none)
                    }
                }
                STOP -> {
                    onStopAction()
                    Snackbar.make(this, "Stop ${scenarioName()}() scenario", Snackbar.LENGTH_LONG).show()
                    setFabTag(START)
                }
            }
        }, this::onError)

        pepper.setOnRobotLost(this::onRobotDisconnected)
    }

    override fun onStart() {
        super.onStart()
        inUISafe({
            fab.visibility = View.INVISIBLE
            pepper.connect()
            onRobotConnected()
        }, this::onError)
    }

    override fun onStop() {
        super.onStop()
        inUISafe({
            onStopAction()
            pepper.disconnect()
        }, this::onError)
    }

    private fun setFabTag(action: String) {
        fab.tag = action
        when (action) {
            "startAction" -> fab.setImageResource(R.drawable.start)
            "stopAction" -> fab.setImageResource(R.drawable.stop)
        }
    }

    protected fun displayInfo(string: String, duration: Int = Snackbar.LENGTH_LONG) {
        Snackbar.make(fab, string, duration).show()
    }

    open fun onError(throwable: Throwable?) {
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

    abstract fun scenarioName(): String


    open fun onRobotConnected() {
        setFabTag(START)
        displayInfo("Pepper is connected!")
        fab.visibility = View.VISIBLE
    }

    open fun onRobotDisconnected(reason: String) {
        inUI {
            setFabTag(STOP)
            displayInfo("Robot Lost : $reason")
            fab.visibility = View.INVISIBLE
        }
    }

    abstract suspend fun onStartAction()

    open suspend fun onStopAction() {
        pepper.stop()
    }
}