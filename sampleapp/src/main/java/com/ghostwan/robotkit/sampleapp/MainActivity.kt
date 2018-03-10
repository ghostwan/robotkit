package com.ghostwan.robotkit.sampleapp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.ghostwan.robotkit.robot.pepper.MyPepper
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        launch(UI) {
            val pepper = MyPepper(this@MainActivity)
            pepper.connect {
                println("Robot Lost : $it")
            }

            pepper.say(R.string.hello_world)
        }

    }
}
