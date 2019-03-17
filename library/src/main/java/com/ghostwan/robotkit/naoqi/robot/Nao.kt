package com.ghostwan.robotkit.naoqi.robot

import android.app.Activity
import com.ghostwan.robotkit.`object`.Action
import com.ghostwan.robotkit.`object`.Result
import com.ghostwan.robotkit.naoqi.NaoqiRobot
import com.ghostwan.robotkit.naoqi.`object`.Discussion
import com.ghostwan.robotkit.naoqi.ext.await
import com.ghostwan.robotkit.naoqi.ext.toNaoqiLocale
import com.ghostwan.robotkit.util.ui

/**
 * Nao robot
 */
open class Nao(activity: Activity, address: String) : NaoqiRobot(activity, address, "nao") {

    override suspend fun discuss(discussion: Discussion, gotoBookmark: String?,
                                 throwOnStop: Boolean,
                                 onStart: (suspend () -> Unit)?,
                                 onResult: (suspend (Result<String>) -> Unit)?
    ): String? {

        //FIXME Weird Hack -_- otherwise the current object die...
        val hack = this
        with(hack) {

            val topics = discussion.topics.map { (key, value) ->
                val top = services.conversation.await().async()?.makeTopic(value).await()
                key to top
            }.toMap()

            val discuss = if (discussion.locale == null) {
                services.conversation.await().async()
                        ?.makeDiscuss(robotContext, topics.values.toList(), getCurrentLocale().toNaoqiLocale()).await()
            } else {
                services.conversation.await().async()
                        ?.makeDiscuss(robotContext, topics.values.toList(), discussion.locale.toNaoqiLocale()).await()
            }

            var startBookmark: String? = null
            discussion.prepare(discuss, topics)?.let {
                startBookmark = it
            }
            gotoBookmark?.let {
                startBookmark = gotoBookmark
            }
            discuss.async().addOnStartedListener {
                ui {
                    onStart?.invoke()
                    if (startBookmark != null) {
                        val bookmark = topics[discussion.mainTopic]?.async()?.bookmarks.await()[startBookmark]
                        discuss.async().goToBookmarkedOutputUtterance(bookmark).await()
                    }
                }
            }.await()

            val future = discuss.async().run()
            return handleFuture(future, onResult, throwOnStop, Action.TALKING, Action.LISTENING, Action.DISCUSSING)
        }
    }

    override fun getRobotType(): String {
        return "Nao"
    }

}