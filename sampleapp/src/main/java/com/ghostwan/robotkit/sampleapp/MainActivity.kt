package com.ghostwan.robotkit.sampleapp

import android.content.res.Resources
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.aldebaran.qi.QiException
import com.ghostwan.robotkit.robot.pepper.MyPepper
import com.ghostwan.robotkit.robot.pepper.Pepper
import com.ghostwan.robotkit.robot.pepper.`object`.Concept
import com.ghostwan.robotkit.robot.pepper.`object`.Discussion
import com.ghostwan.robotkit.robot.pepper.exception.RobotUnavailableException
import com.ghostwan.robotkit.robot.pepper.util.exception
import com.ghostwan.robotkit.robot.pepper.util.info
import com.ghostwan.robotkit.robot.pepper.util.ui
import com.ghostwan.robotkit.robot.pepper.util.uiSafe
import kotlinx.android.synthetic.main.kotlin_activity_main.*
import kotlinx.coroutines.experimental.CancellationException

class MainActivity : AppCompatActivity() {


    private lateinit var pepper: Pepper


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kotlin_activity_main)

        startBtn.setOnClickListener(this::onStartButton)
        stopBtn.setOnClickListener(this::onStopButton)
        discussBtn.setOnClickListener(this::onDiscussButton)
        disconnectBtn.setOnClickListener {
            uiSafe({
                pepper.disconnect()
            }, this::onError)
        }

        pepper = MyPepper(this@MainActivity)
        pepper.setOnRobotLost {
            println("Robot Lost : $it")
            ui {
                hideButton()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        uiSafe({
            pepper.connect()
            isRunning(false)
            disconnectBtn.visibility = View.VISIBLE
        }, this::onError)
    }

    fun onStartButton(view: View) {
        uiSafe({
            isRunning(true)

            pepper.say(R.string.hello_human)

            pepper.animate(R.raw.hello_anim)
            val helloConcept = Concept(this@MainActivity, R.string.hello, R.string.hi)
            val byeConcept = Concept(this@MainActivity, R.string.bye, R.string.see_you)
            val discussConcept = Concept(this@MainActivity, R.string.talk, R.string.discuss)
            val concept = pepper.listen(helloConcept, byeConcept, discussConcept)

            when (concept) {
                helloConcept -> pepper.say(R.string.hello_world, R.raw.hello_anim, R.raw.hello_trajectory)
                byeConcept -> pepper.say(R.string.bye_world, R.raw.bye_anim)
                discussConcept -> {
                    pepper.say("sure  let's talk!")
                    val result = pepper.discuss(R.raw.presentation_discussion, gotoBookmark = "intro")
                    println(result)
                    pepper.say("The discussion end by: $result")
                }
                else -> pepper.say(R.string.i_dont_understand)
            }

            isRunning(false)
        }, this::onError)
    }

    fun onError(throwable: Throwable?) {
        val error = when(throwable){
            is QiException -> {
                hideButton()
                "Robot Exception ${throwable.message}"
            }
            is RobotUnavailableException -> {
                hideButton()
                "Robot unavailble ${throwable.message}"
            }
            is Resources.NotFoundException ->  {
                hideButton()
                "Android resource missing ${throwable.message}"
            }
            is CancellationException -> {
                isRunning(false)
                "Execution was stopped"
            }
            else -> {
                isRunning(false)
                throwable?.message
            }
        }
        exception(throwable, error)
        error?.let { setText(error, true) }
    }

    fun onStopButton(view: View) {
        info( "stopping pepper")
        pepper.stop()
        isRunning(false)
    }

    fun onDiscussButton(view: View) {
        uiSafe({
            info( "starting discussion")
            val discussion = Discussion(this@MainActivity, R.raw.presentation_discussion)
            val result = pepper.discuss(discussion, gotoBookmark = "intro")
            println(result)
            pepper.say("Saving the discussion: ")
            discussion.saveData(this@MainActivity)
        }, this::onError)
    }

    fun isRunning(value: Boolean) {
        if (value) {
            setText("Running...")
            startBtn.visibility = View.INVISIBLE
            discussBtn.visibility = View.INVISIBLE
            stopBtn.visibility = View.VISIBLE
        } else {
            setText("Stopped!")
            startBtn.visibility = View.VISIBLE
            discussBtn.visibility = View.VISIBLE
            stopBtn.visibility = View.INVISIBLE
        }
    }

    fun hideButton() {
        startBtn.visibility = View.INVISIBLE
        stopBtn.visibility = View.INVISIBLE
        discussBtn.visibility = View.INVISIBLE
        disconnectBtn.visibility = View.INVISIBLE
    }

    fun setText(message: String, isError: Boolean = false) {
        textView.text = message
        if (isError) {
            textView.setTextColor(resources.getColor(R.color.error))
        } else {
            textView.setTextColor(resources.getColor(R.color.info))
        }
    }
}
