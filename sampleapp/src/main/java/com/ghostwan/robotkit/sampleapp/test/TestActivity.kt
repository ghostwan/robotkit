package com.ghostwan.robotkit.sampleapp.test

import android.media.MediaPlayer
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.aldebaran.qi.Promise
import com.ghostwan.robotkit.naoqi.`object`.Discussion
import com.ghostwan.robotkit.naoqi.robot.LocalPepper
import com.ghostwan.robotkit.naoqi.robot.Pepper
import com.ghostwan.robotkit.naoqi.robot.isOnLocalPepper
import com.ghostwan.robotkit.sampleapp.R
import com.ghostwan.robotkit.util.getResId
import com.ghostwan.robotkit.util.uiSafe

class TestActivity : AppCompatActivity() {

    companion object {
        const val TAG = "TestActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        val pepper = if (isOnLocalPepper()) {
            LocalPepper(this)
        } else {
            Pepper(this, intent.getStringExtra("address"), "nao")
        }

        uiSafe({
//            test1(pepper)
//            testExecuteQiSDK(pepper, this@TestActivity)
            testExecuteRobotKit(pepper, this@TestActivity)
        }, Tools.Companion::onError)
    }



    private suspend fun testConnectionRobotKit(pepper: Pepper) {
        log("Connecting...")
        pepper.connect()
        Log.i(TAG, "Disconnecting")
        pepper.disconnect()
        Log.i(TAG, "Disconnected")
    }




    private fun execute() {
        val promise = Promise<Boolean>()
        val mediaPlayer = MediaPlayer()
        mediaPlayer.setOnPreparedListener {
            promise.setValue(true)
        }
        mediaPlayer.setOnErrorListener { mp, what, extra ->
            promise.setError("Error code $what")
            true
        }
        mediaPlayer.start()
        promise.setOnCancel {
            mediaPlayer.stop()
        }
        promise.future.get()
    }


    private suspend fun test1(pepper: Pepper) {
        var step = 0
        log("test connection")
        testConnectionRobotKit(pepper)
        log("test connection")
        testConnectionRobotKit(pepper)

        log("tests")
        pepper.connect()

        log("animate")
        pepper.animate(R.raw.bye_anim)
        log("say")
        pepper.say("say ${++step} ")
        log("say")
        pepper.say("say ${++step} ")

        log("say and animate")
        pepper.say(R.string.hello_world, R.raw.hello_anim)

        log("stopping...")
        pepper.stop()
        log("disconnecting...")
        pepper.disconnect()

//        log(" test naoqi pure API: Say")
//        val conversation = pepper.services.conversation.await()
//        val robotContext = pepper.robotContext
//        val serializer = pepper.services.serializer
//
//        val say = conversation.async().makeSay(robotContext, Phrase("bye")).await()
//        say.async().run().await()
//
//        log(" test naoqi pure API: Chat")
//        val topic = conversation.async().makeTopic(getRaw(R.raw.test_topic)).await()
//        val qiChatBot = conversation.async().makeQiChatbot(robotContext, listOf(topic)).await()
//        val chat = conversation.async().makeChat(robotContext, listOf(qiChatBot)).await()
//
//        val qiChatExecutor = object : RKQiChatExecutor(serializer) {
//            override fun runWith(params: List<String>) {
//                //Do something
//            }
//
//            override fun stop() {
//                //Stop what we are doing
//            }
//        }
//        qiChatBot.async().setExecutors(mapOf(Pair("animate", qiChatExecutor))).await()
//        val startBook = topic.bookmarks["start"]
//        qiChatBot.async().goToBookmark(startBook, AutonomousReactionImportance.HIGH, AutonomousReactionValidity.IMMEDIATE)
//        val chating =  chat.async().run()
//        qiChatBot.addOnEndedListener {
//            chating.requestCancellation()
//        }
//        chating.await()
//
//        finish()
    }


}
