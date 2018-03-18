package com.ghostwan.robotkit.sampleapp

import android.content.res.Resources
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.aldebaran.qi.QiException
import com.ghostwan.robotkit.robot.pepper.MyPepper
import com.ghostwan.robotkit.robot.pepper.MyPepper.Companion.info
import com.ghostwan.robotkit.robot.pepper.Pepper
import com.ghostwan.robotkit.robot.pepper.`object`.Discussion
import com.ghostwan.robotkit.robot.pepper.exception.RobotUnavailableException
import com.ghostwan.robotkit.robot.pepper.ui
import com.ghostwan.robotkit.robot.pepper.uiSafe
import kotlinx.android.synthetic.main.activity_test.*
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async

class TestActivity : AppCompatActivity() {

    private lateinit var pepper: Pepper
    private lateinit var discussion : Discussion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        pepper = MyPepper(this)
        discussion = Discussion(this, R.raw.test_discussion)
        discussion.setOnBookmarkReached { info("Bookmark $it reached!") }
        discussion.setOnVariableChanged { name, value -> info("Variable $name changed to $value") }
        clearDataBtn.setOnClickListener {
            uiSafe ({
                discussion.clearData()
                pepper.stop()
                pepper.discuss(discussion, gotoBookmark = "intro" , onStart = {
                    gotoBookmarkBtn.visibility = View.VISIBLE
                })
            }, onError = {
              it?.printStackTrace()
            })
        }
        gotoBookmarkBtn.setOnClickListener{
            ui{
                discussion.gotoBookmark( "testGoto")
            }
        }
        pepper.setOnRobotLost {
            info("Robot Lost : $it")
        }
    }

    override fun onStart() {
        super.onStart()
        gotoBookmarkBtn.visibility = View.INVISIBLE
        uiSafe (onRun = {
            pepper.connect()

            val t1 = async(UI) { pepper.say("hello world") }
            val t2 = async(UI) { pepper.animate(R.raw.hello_anim) }

            t1.await()
            t2.await()

            val result = if(discussion.restoreData(this@TestActivity)) {
                pepper.discuss(discussion, onStart = {
                    gotoBookmarkBtn.visibility = View.VISIBLE
                })
            }
            else {
                pepper.discuss(discussion, gotoBookmark = "intro", onStart = {
                    gotoBookmarkBtn.visibility = View.VISIBLE
                })
            }
            println("End discuss : step $result")
            gotoBookmarkBtn.visibility = View.INVISIBLE
        }, onError = {
            when(it){
                is QiException -> println("Robot Exception ${it.message}")
                is RobotUnavailableException -> println("Robot unavailble ${it.message}")
                is Resources.NotFoundException ->  println("Android resource missing ${it.message}")
                is CancellationException -> println("Execution was stopped")
                else -> it?.printStackTrace()
            }
        })
    }

    override fun onStop() {
        super.onStop()
        uiSafe({
            info("user name is ${discussion.getVariable("name")}")
            discussion.saveData(this@TestActivity)
        }, {})
    }


}
