package com.ghostwan.robotkit.sampleapp.samples

import com.ghostwan.robotkit.naoqi.`object`.ResConcept
import com.ghostwan.robotkit.sampleapp.R
import com.ghostwan.robotkit.sampleapp.helpers.MultiLocaleActivity

class ListenActivity : MultiLocaleActivity() {


    override fun scenarioName() = "Listen"

    override suspend fun onStartAction() {
        robot.say(R.string.hello_human, locale = locale)
        val helloConcept = ResConcept(R.string.hello, R.string.hi)
        val byeConcept = ResConcept(R.string.bye, R.string.see_you)
        val discussConcept = ResConcept(R.string.talk, R.string.discuss)

        val concept = robot.listen(helloConcept, byeConcept, discussConcept, locale = locale)

        when (concept) {
            helloConcept -> robot.say(R.string.hello_world, R.raw.hello_anim, R.raw.hello_trajectory, locale = locale)
            byeConcept -> robot.say(R.string.bye_world, R.raw.bye_anim, locale = locale)
            discussConcept -> {
                robot.say(R.string.lets_talk, locale = locale)
                val result = robot.discuss(R.raw.presentation_discussion, gotoBookmark = "intro", locale = locale)
                println(result)
                robot.say("The discussion end by: $result", locale = locale)
            }
            else -> robot.say(R.string.i_dont_understand)
        }
    }

}
