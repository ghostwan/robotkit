package com.ghostwan.robotkit.sampleapp.samples

import android.view.View
import com.ghostwan.robotkit.ext.setOnClickCoroutine
import com.ghostwan.robotkit.naoqi.`object`.Discussion
import com.ghostwan.robotkit.sampleapp.R
import com.ghostwan.robotkit.sampleapp.helpers.MultiLocaleActivity
import com.ghostwan.robotkit.sampleapp.helpers.playSound
import com.ghostwan.robotkit.sampleapp.helpers.stopSound
import com.ghostwan.robotkit.util.infoLog
import com.ghostwan.robotkit.util.ui
import kotlinx.android.synthetic.main.activity_discuss.*

class DiscussActivity : MultiLocaleActivity() {

    override fun scenarioName(): String = "Discuss"
    override fun defaultLayout(): Int = R.layout.activity_discuss

    private lateinit var discussion: Discussion

    override fun onRobotConnected() {
        super.onRobotConnected()
        ui {
            gotoBookmarkBtn.visibility = View.VISIBLE
        }
    }

    override fun onRobotDisconnected(reason: String) {
        super.onRobotDisconnected(reason)
        ui {
            gotoBookmarkBtn.visibility = View.INVISIBLE
        }
    }

    override suspend fun onStartAction() {
        discussion = Discussion(this, R.raw.presentation_discussion, locale = locale)
        discussion.setOnBookmarkReached { infoLog("Bookmark $it reached!") }
        discussion.setOnVariableChanged { name, value -> infoLog("Variable $name changed to $value") }
        clearDataBtn.setOnClickCoroutine {
            discussion.clearData()
            displayInfo("Data cleared!")
            onStopAction()
        }
        gotoBookmarkBtn.setOnClickCoroutine {
            discussion.gotoBookmark("testGoto")
        }

        gotoBookmarkBtn.visibility = View.INVISIBLE

        val result = if (discussion.restoreData(this)) {
            pepper.say(R.string.restore_discussion, locale = locale)
            playSound(this)
            pepper.discuss(discussion, onStart = {
                gotoBookmarkBtn.visibility = View.VISIBLE
                stopSound()
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
        discussion.saveData(this)
    }

}
