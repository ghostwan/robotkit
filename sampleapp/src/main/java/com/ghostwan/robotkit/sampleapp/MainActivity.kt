package com.ghostwan.robotkit.sampleapp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.ghostwan.robotkit.robot.pepper.MyPepper
import com.ghostwan.robotkit.robot.pepper.`object`.Concept
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class MainActivity : AppCompatActivity() {


    private lateinit var pepper : MyPepper

    companion object {
        val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pepper = MyPepper(this@MainActivity)
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

                pepper.say(R.string.hello_human)

                val helloConcept = Concept(this@MainActivity, R.string.hello, R.string.hi)
                val byeConcept = Concept(this@MainActivity, R.string.bye, R.string.see_you)
                val concept = pepper.listen(helloConcept, byeConcept)
                when (concept) {
                    helloConcept -> pepper.say(R.string.hello_world, R.raw.waving_both_hands_b003)
                    byeConcept -> pepper.say(R.string.bye_world, R.raw.salute_right_b001)
                    else -> pepper.say(R.string.i_dont_understood)
                }

                isRunning(false)
            } catch (e : Exception){
                Log.e(TAG, "Something happened!", e)
                isRunning(false)
                e.message?.let { setText(it, true) }
            }
        }
    }

    fun setText(message : String, isError : Boolean = false) {
        textView.text = message
        if (isError) {
            textView.setTextColor(resources.getColor(android.R.color.holo_red_light))
        }
        else {
            textView.setTextColor(resources.getColor(android.R.color.holo_blue_light))
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
}
