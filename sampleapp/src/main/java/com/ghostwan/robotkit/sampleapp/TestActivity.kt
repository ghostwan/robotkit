package com.ghostwan.robotkit.sampleapp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.ghostwan.robotkit.robot.pepper.MyPepper
import com.ghostwan.robotkit.robot.pepper.MyPepper.Companion.ui
import com.ghostwan.robotkit.robot.pepper.`object`.Discussion
import kotlinx.android.synthetic.main.activity_test.*

class TestActivity : AppCompatActivity() {

    private lateinit var pepper: MyPepper
    private lateinit var discussion : Discussion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        pepper = MyPepper(this)
        discussion = Discussion(this, R.raw.presentation_discussion)
        clearDataBtn.setOnClickListener {
            ui {
                discussion.clearData()
                pepper.stop()
                pepper.discuss(discussion, gotoBookmark = "intro")
            }
        }
        pepper.setOnRobotLost {
            println("Robot Lost : $it")
        }
    }

    override fun onStart() {
        super.onStart()
        ui {
            if(!pepper.isConnected()) {
                pepper.connect()
            }
            try {
                val result = if(discussion.restoreData(this@TestActivity)) {
                    pepper.discuss(discussion)
                }
                else {
                    pepper.discuss(discussion, gotoBookmark = "intro")
                }
                println("End by $result")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        ui{
            discussion.saveData(this@TestActivity)
        }
    }


}
