package com.ghostwan.robotkit.sampleapp

import android.content.res.Resources
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.aldebaran.qi.QiException
import com.ghostwan.robotkit.robot.pepper.MyPepper
import com.ghostwan.robotkit.robot.pepper.Pepper
import com.ghostwan.robotkit.robot.pepper.exception.RobotUnavailableException
import com.ghostwan.robotkit.robot.pepper.util.ui
import com.ghostwan.robotkit.robot.pepper.util.uiSafe
import kotlinx.android.synthetic.main.activity_parent.*
import kotlinx.coroutines.experimental.CancellationException

/**
 * Created by erwan on 20/03/2018.
 */
abstract class ParentActivity : AppCompatActivity()  {

    companion object {
        const val START = "startAction"
        const val STOP = "stopAction"
    }

    internal lateinit var pepper: Pepper

    protected open fun layout () : Int {
        return R.layout.activity_parent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout())

        fab.setOnClickListener { view ->
            when(fab.tag) {
                START -> {
                    start()
                    Snackbar.make(view, "Start ${scenarioName()}() scenario", Snackbar.LENGTH_LONG).show()
                    setFabTag(STOP)
                }
                STOP -> {
                    stop()
                    Snackbar.make(view, "Stop ${scenarioName()}() scenario", Snackbar.LENGTH_LONG).show()
                    setFabTag(START)
                }
            }
        }


        pepper = MyPepper(this)
        pepper.setOnRobotLost {
            displayInfo("Robot Lost : $it")
            ui {
                fab.visibility = View.INVISIBLE
            }
        }
    }

    override fun onStart() {
        super.onStart()
        uiSafe({
            fab.visibility = View.INVISIBLE
            pepper.connect()
            setFabTag(START)
            displayInfo("Pepper is connected!")
            fab.visibility = View.VISIBLE
        }, this::onError)
    }

    private fun setFabTag(action : String) {
        fab.tag = action
        when(action) {
            "startAction" -> fab.setImageResource(R.drawable.start)
            "stopAction" -> fab.setImageResource(R.drawable.stop)
        }
    }

    protected fun displayInfo(string : String, duration : Int = Snackbar.LENGTH_LONG) {
        Snackbar.make(fab, string, duration).show()
    }

    fun onError(throwable: Throwable?) {
        val message = when(throwable){
            is QiException -> "Robot Exception ${throwable.message}"
            is RobotUnavailableException -> "Robot unavailble ${throwable.message}"
            is Resources.NotFoundException -> "Android resource missing ${throwable.message}"
            is CancellationException ->  "Execution was stopped"
            else -> throwable?.message
        }
        throwable?.printStackTrace()
        message?.let { displayInfo(message) }
    }

    fun stop() {
        pepper.stop()
    }

    abstract fun start()

    abstract fun scenarioName() : String
}