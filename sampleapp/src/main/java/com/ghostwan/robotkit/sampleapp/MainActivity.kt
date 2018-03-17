package com.ghostwan.robotkit.sampleapp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.ghostwan.robotkit.robot.pepper.MyPepper
import com.ghostwan.robotkit.robot.pepper.`object`.Concept
import com.ghostwan.robotkit.robot.pepper.`object`.Discussion
import kotlinx.android.synthetic.main.kotlin_activity_main.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class MainActivity : AppCompatActivity() {


    private lateinit var pepper : MyPepper

    companion object {
        val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kotlin_activity_main)

        pepper = MyPepper(this@MainActivity)
        pepper.setOnRobotLost {
            println("Robot Lost : $it")
            launch (UI){
                startBtn.visibility = View.INVISIBLE
                stopBtn.visibility = View.INVISIBLE
                discussBtn.visibility = View.INVISIBLE
            }
        }
    }

    override fun onStart() {
        super.onStart()
        launch(UI) {
            if(!pepper.isConnected()) {
                pepper.connect()
                isRunning(false)
            }
        }
    }

    fun onStartButton(view : View) {
        launch(UI) {
            try {
                isRunning(true)

                pepper.say(R.string.hello_human)

                pepper.animate(R.raw.hello_anim)
                val helloConcept= Concept(this@MainActivity, R.string.hello, R.string.hi)
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
                        pepper.say("The discussion end by: "+result)
                    }
                    else -> pepper.say(R.string.i_dont_understand)
                }

                isRunning(false)
            } catch (e : Exception){
                Log.e(TAG, "Something happened!", e)
                isRunning(false)
                e.message?.let { setText(it, true) }
            }
        }
    }

    fun onStopButton(view : View) {
        launch (UI){
            Log.i(TAG, "stopping pepper")
            pepper.stop()
            isRunning(false)
        }
    }

    fun onDiscussButton(view: View){
        launch (UI){
            Log.i(TAG, "starting discussion")
            val discussion = Discussion(this@MainActivity, R.raw.presentation_discussion)
            val result = pepper.discuss(discussion, gotoBookmark = "intro")
            println(result)
            pepper.say("Saving the discussion: ")
            discussion.saveData(this@MainActivity)
        }
    }

    fun isRunning(value: Boolean) {
        if(value) {
            setText("Running...")
            startBtn.visibility = View.INVISIBLE
            discussBtn.visibility = View.INVISIBLE
            stopBtn.visibility = View.VISIBLE
        }
        else {
            setText("Stopped!")
            startBtn.visibility = View.VISIBLE
            discussBtn.visibility = View.VISIBLE
            stopBtn.visibility = View.INVISIBLE
        }
    }

    fun setText(message : String, isError : Boolean = false) {
        textView.text = message
        if (isError) {
            textView.setTextColor(resources.getColor(R.color.error))
        }
        else {
            textView.setTextColor(resources.getColor(R.color.info))
        }
    }
}
