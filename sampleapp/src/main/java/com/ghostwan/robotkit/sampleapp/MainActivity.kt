package com.ghostwan.robotkit.sampleapp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.ghostwan.robotkit.robot.pepper.MyPepper
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
    }

    override fun onStart() {
        super.onStart()
        launch(UI) {
            if(!pepper.isConnected()) {
                pepper.connect {
                    println("Robot Lost : $it")
                    launch (UI){
                        start_bt.visibility = View.INVISIBLE
                        stop_bt.visibility = View.INVISIBLE
                    }
                }
                start_bt.visibility = View.VISIBLE
                stop_bt.visibility = View.VISIBLE
            }
        }
    }

    fun onStartButton(view : View){
        launch(UI) {
            try {
                view.isEnabled = false
                Log.i(TAG, "saying hello world")
                pepper.say(R.string.hello_world)
                Log.i(TAG, "saying bye world")
                pepper.say(R.string.bye_world)
                Log.i(TAG, "Pepper said everything")
                view.isEnabled = true
            } catch (e : Exception){
                Log.e(TAG, "Something happened!", e)
            }

        }
    }
    fun onStopButton(view : View) {
        launch (UI){
            Log.i(TAG, "stopping pepper")
            pepper.stop()
            start_bt.isEnabled = true
        }
    }
}
