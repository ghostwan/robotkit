package com.ghostwan.robotkit.sampleapp.test.kotlin

import android.content.Context
import com.aldebaran.qi.Future
import com.aldebaran.qi.Promise
import com.aldebaran.qi.sdk.`object`.conversation.AutonomousReactionImportance
import com.aldebaran.qi.sdk.`object`.conversation.AutonomousReactionValidity
import com.aldebaran.qi.sdk.`object`.conversation.QiChatExecutor
import com.ghostwan.robotkit.`object`.Action
import com.ghostwan.robotkit.ext.getRaw
import com.ghostwan.robotkit.naoqi.`object`.AsyncQiChatExecutor
import com.ghostwan.robotkit.naoqi.`object`.Discussion
import com.ghostwan.robotkit.naoqi.ext.await
import com.ghostwan.robotkit.naoqi.robot.Pepper
import com.ghostwan.robotkit.sampleapp.R
import com.ghostwan.robotkit.sampleapp.helpers.playSound
import com.ghostwan.robotkit.sampleapp.helpers.stopSound

const val testName = "Chat / Executor"

internal suspend fun testExecuteRobotKit(pepper: Pepper, context: Context) {
    start(testName)
    pepper.connect()

    val discussion = Discussion(context, R.raw.test_topic)

    discussion.setAsyncExecutor(pepper, "animate", onExecute = {
    pepper.animate(R.raw.hello_anim)
        playSound(context)
        Promise<Void>().future
    }, onStopExecute = {
        pepper.stopAllBut(Action.DISCUSSING)
        stopSound()
    })
    pepper.discuss(discussion, gotoBookmark = "start")
    pepper.disconnect()
    end(testName)
}


internal suspend fun testExecuteQiSDK(pepper: Pepper, context: Context) {
    start(testName)

    pepper.connect()

    val conversation = pepper.services.conversation.await()
    val robotContext = pepper.robotContext
    val serializer = pepper.services.serializer

    val promise = Promise<Void>()
    val topic = conversation.async().makeTopic(context.getRaw(R.raw.test_topic)).await()
    val qiChatBot = conversation.async().makeQiChatbot(robotContext, listOf(topic)).await()
    val chat = conversation.async().makeChat(robotContext, listOf(qiChatBot)).await()
    val qiChatExecutor = AsyncQiChatExecutor(serializer, object : QiChatExecutor.Async {
        override fun runWith(params: List<String>): Future<Void>? {
            log("On running!")
            playSound(context)
            promise.setOnCancel {
                //remove listner
            }
            return promise.future
        }

        override fun stop(): Future<Void>? {
            log("Stopping...")
            stopSound()
            log("Stopped!")
            return Future.of(null)
        }
    })
    qiChatBot.async().setExecutors(mapOf(Pair("animate", qiChatExecutor))).await()
    val startBook = topic.async().bookmarks.await()["start"]
    qiChatBot.async().goToBookmark(startBook, AutonomousReactionImportance.HIGH, AutonomousReactionValidity.IMMEDIATE)
    val chating = chat.async().run()
    qiChatBot.async().addOnEndedListener {
        chating.requestCancellation()
    }.await()
    try {
        chating.await()
    } catch (e: Exception) {
        Tools.onError(e)
    }

    pepper.disconnect()
    end(testName)
}