package com.ghostwan.robotkit.sampleapp.scenario

import com.ghostwan.robotkit.robot.pepper.`object`.ResConcept
import com.ghostwan.robotkit.robot.pepper.util.uiSafe
import com.ghostwan.robotkit.sampleapp.ParentActivity
import com.ghostwan.robotkit.sampleapp.R
import java.util.*

class ListenActivity : ParentActivity() {

    override fun scenarioName() = "Listen"

    override fun start() {
        uiSafe({
            pepper.say(R.string.hello_human)
            val helloConcept = ResConcept(R.string.hello, R.string.hi)
            val byeConcept = ResConcept(R.string.bye, R.string.see_you)
            val discussConcept = ResConcept(R.string.talk, R.string.discuss)

            val concept = pepper.listen(helloConcept, byeConcept, discussConcept, locale = Locale.FRENCH)

            when (concept) {
                helloConcept -> pepper.say(R.string.hello_world, R.raw.hello_anim, R.raw.hello_trajectory)
                byeConcept -> pepper.say(R.string.bye_world, R.raw.bye_anim)
                discussConcept -> {
                    pepper.say("sure  let's talk!")
                    val result = pepper.discuss(R.raw.presentation_discussion, gotoBookmark = "intro")
                    println(result)
                    pepper.say("The discussion end by: $result")
                }
                else -> pepper.say(R.string.i_dont_understand)
            }
        }, this::onError)
    }



}
