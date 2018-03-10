package com.ghostwan.robotkit.robot.pepper

import android.app.Activity
import android.content.Context
import android.util.Log
import com.aldebaran.qi.Future
import com.aldebaran.qi.Session
import com.aldebaran.qi.sdk.`object`.context.RobotContext
import com.aldebaran.qi.sdk.`object`.conversation.Conversation
import com.aldebaran.qi.sdk.`object`.conversation.Phrase
import com.aldebaran.qi.sdk.core.FocusManager
import com.aldebaran.qi.sdk.core.SessionManager
import com.ghostwan.robotkit.robot.pepper.ext.await
import java.util.concurrent.CancellationException


/**
 * Created by erwan on 10/03/2018.
 */
class MyPepper(activity: Activity) {

    private var futures : MutableList<Future<*>> = ArrayList()
    private var util : PepperUtil = PepperUtil()
    private var sessionManager: SessionManager = SessionManager(false)
    private var focusManager: FocusManager = FocusManager(activity, sessionManager)
    private var context: Context = activity
    private var session: Session? = null
    private var robotContext: RobotContext? = null
    private var conversation: Conversation? = null

    companion object {
        val TAG = "MyPepper"
    }

    suspend fun connect(callback: (String) -> Unit) {
        robotContext = util.deserializeRobotContext(focusManager.await(callback))
        Log.i(TAG, "focus retrieved")
        session = sessionManager.await(context, callback)
        Log.i(TAG, "session connected")
        conversation = util.retrieveService(session!!, Conversation::class.java, "Conversation")
    }

    fun isConnected(): Boolean = session != null && session!!.isConnected

    private suspend fun <T : Any?> handleFuture(future: Future<T>, wait: Boolean, throwOnCancel:Boolean) {
        futures.add(future)
        future.thenConsume { futures.remove(future) }
        if(wait) {
            try {
                future.await()
            } catch (e : CancellationException){
                if(throwOnCancel)
                    throw e
            }
        }
    }
    suspend fun stop() {
        Log.i(TAG, "cancelling ${futures.size} futures")
        for (future in futures) {
            future.requestCancellation();
            futures.remove(future)
        }
    }

    suspend fun say(res: Int, wait: Boolean = true, throwOnCancel:Boolean = true) {
        val say = conversation?.async()?.makeSay(robotContext, Phrase(context.getString(res))).await()
        val futureSay = say.async().run()
        handleFuture(futureSay, wait, throwOnCancel)
    }


}