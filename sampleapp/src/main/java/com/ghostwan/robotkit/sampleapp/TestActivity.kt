package com.ghostwan.robotkit.sampleapp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.ghostwan.robotkit.robot.pepper.MyPepper
import com.ghostwan.robotkit.robot.pepper.MyPepper.Companion.exception
import com.ghostwan.robotkit.robot.pepper.MyPepper.Companion.info
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
        discussion = Discussion(this, R.raw.test_discussion)
        discussion.setOnBookmarkReached { info("Bookmark $it reached!") }
        discussion.setOnVariableChanged { name, value -> info("Variable $name changed to $value") }
        clearDataBtn.setOnClickListener {
            ui {
                discussion.clearData()
                pepper.stop()
                pepper.discuss(discussion, gotoBookmark = "intro" , onStart = {
                    gotoBookmarkBtn.visibility = View.VISIBLE
                })
            }
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
        ui {
            if(!pepper.isConnected()) {
                pepper.connect()
            }
            try {
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
            } catch (e: Exception) {
                exception(e)
            }
            gotoBookmarkBtn.visibility = View.INVISIBLE
        }
    }

    override fun onStop() {
        super.onStop()
        ui{
            try {
                info("user name is ${discussion.getVariable("name")}")
                discussion.saveData(this@TestActivity)
            }
            catch (e : Exception) {
                exception(e)
            }
        }
    }


}
