package com.ghostwan.robotkit.sampleapp.samples

import com.ghostwan.robotkit.naoqi.`object`.ResConcept
import com.ghostwan.robotkit.sampleapp.helpers.MultiLocaleActivity
import com.ghostwan.robotkit.sampleapp.R

class ListenActivity : MultiLocaleActivity() {


    override fun scenarioName() = "Listen"

    override suspend fun onStartAction() {
        pepper.say(R.string.hello_human, locale = locale)
        val helloConcept = ResConcept(R.string.hello, R.string.hi)
        val byeConcept = ResConcept(R.string.bye, R.string.see_you)
        val discussConcept = ResConcept(R.string.talk, R.string.discuss)

        val concept = pepper.listen(helloConcept, byeConcept, discussConcept, locale = locale)

        when (concept) {
            helloConcept -> pepper.say(R.string.hello_world, R.raw.hello_anim, R.raw.hello_trajectory, locale = locale)
            byeConcept -> pepper.say(R.string.bye_world, R.raw.bye_anim, locale = locale)
            discussConcept -> {
                pepper.say(R.string.lets_talk, locale = locale)
                val result = pepper.discuss(R.raw.presentation_discussion, gotoBookmark = "intro", locale = locale)
                println(result)
                pepper.say("The discussion end by: $result", locale = locale)
            }
            else -> pepper.say(R.string.i_dont_understand)
        }
    }

}
