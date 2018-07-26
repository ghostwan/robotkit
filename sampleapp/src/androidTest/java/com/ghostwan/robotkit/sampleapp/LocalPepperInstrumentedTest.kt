package com.ghostwan.robotkit.sampleapp

import android.support.test.filters.MediumTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.ghostwan.robotkit.naoqi.robot.LocalPepper
import com.ghostwan.robotkit.util.ui
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@MediumTest
class LocalPepperInstrumentedTest {

    @get:Rule
    var rule: ActivityTestRule<TestInstrumentedActivity> = ActivityTestRule(TestInstrumentedActivity::class.java)

    @Test
    fun pepper_can_connect() {
        val pepper = LocalPepper(rule.activity)
        ui {
            pepper.connect()
            pepper.say("It's working!")
        }
    }

    @Test
    fun pepper_can_disconnect() {
        val pepper = LocalPepper(rule.activity)
        ui {
            pepper.connect()
            pepper.say("It's working!")
            pepper.disconnect()
            pepper.say("It'shouldn't work!")
        }
    }

    @Test
    fun pepper_can_reconnect() {
        val pepper = LocalPepper(rule.activity)
        ui {
            pepper.connect()
            pepper.say("It's working!")
            pepper.disconnect()
            pepper.say("It'shouldn't work!")
            pepper.connect()
            pepper.say("It's working!")
        }
    }

}