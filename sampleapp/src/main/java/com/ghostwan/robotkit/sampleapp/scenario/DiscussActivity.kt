package com.ghostwan.robotkit.sampleapp.scenario

import android.view.View
import com.ghostwan.robotkit.robot.pepper.`object`.Discussion
import com.ghostwan.robotkit.robot.pepper.util.info
import com.ghostwan.robotkit.robot.pepper.util.ui
import com.ghostwan.robotkit.robot.pepper.util.uiSafe
import com.ghostwan.robotkit.sampleapp.MultiLocaleActivity
import com.ghostwan.robotkit.sampleapp.R
import kotlinx.android.synthetic.main.activity_discuss.*

class DiscussActivity : MultiLocaleActivity() {

    override fun scenarioName(): String = "Discuss"
    override fun defaultLayout(): Int = R.layout.activity_discuss

    private lateinit var discussion: Discussion

    override fun onRobotConnected() {
        super.onRobotConnected()
        gotoBookmarkBtn.visibility = View.VISIBLE
    }

    override fun onRobotDisconnected(reason: String) {
        super.onRobotDisconnected(reason)
        gotoBookmarkBtn.visibility = View.INVISIBLE
    }
    override suspend fun onStartAction() {
        discussion = Discussion(this, R.raw.presentation_discussion, locale = locale)
        discussion.setOnBookmarkReached { info("Bookmark $it reached!") }
        discussion.setOnVariableChanged { name, value -> info("Variable $name changed to $value") }
        clearDataBtn.setOnClickListener {
            uiSafe({
                discussion.clearData()
                displayInfo("Data cleared!")
                onStopAction()
            }, onError = {
                it?.printStackTrace()
            })
        }
        gotoBookmarkBtn.setOnClickListener {
            ui {
                discussion.gotoBookmark("testGoto")
            }
        }

        gotoBookmarkBtn.visibility = View.INVISIBLE

        val result = if (discussion.restoreData(this)) {
            pepper.say(R.string.restore_discussion, locale = locale)
            pepper.discuss(discussion, onStart = {
                gotoBookmarkBtn.visibility = View.VISIBLE
            })
        } else {
            pepper.discuss(discussion, gotoBookmark = "intro", onStart = {
                gotoBookmarkBtn.visibility = View.VISIBLE
            })
        }
        displayInfo("End discuss : step $result")
        gotoBookmarkBtn.visibility = View.INVISIBLE
    }

    override suspend fun onStopAction() {
        super.onStopAction()
        displayInfo("user name is ${discussion.getVariable("name")}")
        discussion.saveData(this@DiscussActivity)
    }



}
