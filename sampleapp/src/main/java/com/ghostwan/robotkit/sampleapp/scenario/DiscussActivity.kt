package com.ghostwan.robotkit.sampleapp.scenario

import android.view.View
import com.ghostwan.robotkit.robot.pepper.`object`.Discussion
import com.ghostwan.robotkit.robot.pepper.util.info
import com.ghostwan.robotkit.robot.pepper.util.ui
import com.ghostwan.robotkit.robot.pepper.util.uiAsync
import com.ghostwan.robotkit.robot.pepper.util.uiSafe
import com.ghostwan.robotkit.sampleapp.ParentActivity
import com.ghostwan.robotkit.sampleapp.R
import kotlinx.android.synthetic.main.activity_discuss.*

class DiscussActivity : ParentActivity() {

    override fun scenarioName(): String = "Discuss"
    override fun layout() : Int = R.layout.activity_discuss

    private lateinit var discussion : Discussion

    override fun start() {
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

        gotoBookmarkBtn.visibility = View.INVISIBLE
        uiSafe (onRun = {
            pepper.connect()

            val t1 = uiAsync { pepper.say("hello world") }
            val t2 = uiAsync { pepper.animate(R.raw.hello_anim) }

            t1.await()
            t2.await()

            val result = if(discussion.restoreData(this@DiscussActivity)) {
                pepper.discuss(discussion, onStart = {
                    gotoBookmarkBtn.visibility = View.VISIBLE
                })
            }
            else {
                pepper.discuss(discussion, gotoBookmark = "intro", onStart = {
                    gotoBookmarkBtn.visibility = View.VISIBLE
                })
            }
            displayInfo("End discuss : step $result")
            gotoBookmarkBtn.visibility = View.INVISIBLE
        }, onError = this::onError)
    }


    override fun onStop() {
        super.onStop()
        uiAsync {
            displayInfo("user name is ${discussion.getVariable("name")}")
            discussion.saveData(this@DiscussActivity)
        }
    }


}
