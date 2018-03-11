package com.ghostwan.robotkit.sampleapp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.ghostwan.robotkit.robot.pepper.MyPepper
import com.ghostwan.robotkit.robot.pepper.`object`.Concept
import kotlinx.android.synthetic.main.kotlin_activity_main.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class KotlinMainActivity : AppCompatActivity() {


    private lateinit var pepper : MyPepper

    companion object {
        val TAG = "KotlinMainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kotlin_activity_main)

        pepper = MyPepper(this@KotlinMainActivity)
        pepper.setRobotLostListener {
            println("Robot Lost : $it")
            launch (UI){
                start_bt.visibility = View.INVISIBLE
                stop_bt.visibility = View.INVISIBLE
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

    fun onStartButton(view : View){
        launch(UI) {
            try {
                isRunning(true)

                pepper.animate(R.raw.intro_anim)
                pepper.say(R.string.hello_human)

                val helloConcept = Concept(this@KotlinMainActivity, R.string.hello, R.string.hi)
                val byeConcept = Concept(this@KotlinMainActivity, R.string.bye, R.string.see_you)
                val concept = pepper.listen(helloConcept, byeConcept)
                when (concept) {
                    helloConcept -> pepper.say(R.string.hello_world, R.raw.hello_anim)
                    byeConcept -> pepper.say(R.string.bye_world, R.raw.bye_anim)
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

    fun isRunning(value: Boolean) {
        if(value) {
            setText("Running...")
            start_bt.visibility = View.INVISIBLE
            stop_bt.visibility = View.VISIBLE
        }
        else {
            setText("Stopped!")
            start_bt.visibility = View.VISIBLE
            stop_bt.visibility = View.INVISIBLE
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
