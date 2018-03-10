package com.ghostwan.robotkit.robot.pepper

import android.app.Activity
import android.content.Context
import android.util.Log
import com.aldebaran.qi.Future
import com.aldebaran.qi.Session
import com.aldebaran.qi.sdk.`object`.context.RobotContext
import com.aldebaran.qi.sdk.`object`.conversation.BodyLanguageOption
import com.aldebaran.qi.sdk.`object`.conversation.Conversation
import com.aldebaran.qi.sdk.`object`.conversation.Phrase
import com.aldebaran.qi.sdk.`object`.conversation.PhraseSet
import com.aldebaran.qi.sdk.core.FocusManager
import com.aldebaran.qi.sdk.core.SessionManager
import com.ghostwan.robotkit.robot.pepper.`object`.Concept
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
        Log.i(TAG, "session connected")
        session = sessionManager.await(context, callback)
        Log.i(TAG, "focus retrieved")
        conversation = util.retrieveService(session!!, Conversation::class.java, "Conversation")
    }

    fun isConnected(): Boolean = session != null && session!!.isConnected

    private suspend fun <T : Any?> handleFuture(future: Future<T>, wait: Boolean, throwOnCancel:Boolean): T? {
        futures.add(future)
        future.thenConsume { futures.remove(future) }
        if(wait) {
            try {
                return future.await()
            } catch (e : CancellationException){
                if(throwOnCancel)
                    throw e
            }
        }
        return null
    }
    suspend fun stop() {
        Log.i(TAG, "cancelling ${futures.size} futures")
        for (future in futures) {
            future.requestCancellation();
            futures.remove(future)
        }
    }

    suspend fun say(res: Int, bodyLanguageOption: BodyLanguageOption? =null, wait: Boolean = true, throwOnCancel:Boolean = true) {
        val say = if(bodyLanguageOption != null)
            conversation?.async()?.makeSay(robotContext, Phrase(context.getString(res)), bodyLanguageOption).await()
        else
            conversation?.async()?.makeSay(robotContext, Phrase(context.getString(res))).await()

        val future = say.async().run()
        handleFuture(future, wait, throwOnCancel)
    }

    suspend fun listen(vararg concepts: Concept, bodyLanguageOption: BodyLanguageOption? =null, throwOnCancel:Boolean = true): Concept? {
        val phraseSets : MutableList<PhraseSet> = ArrayList()
        concepts.mapTo(phraseSets) { conversation?.async()?.makePhraseSet(it.phrases).await() }

        val listen = if(bodyLanguageOption != null)
            conversation?.async()?.makeListen(robotContext, phraseSets, bodyLanguageOption).await()
        else
            conversation?.async()?.makeListen(robotContext, phraseSets).await()

        val future = listen.async().run()
        val listenResult = handleFuture(future, true, throwOnCancel)

        listenResult?.heardPhrase.let {
            concepts
                    .filter { concept -> concept.isPhraseInConcept(it!!) }
                    .forEach { return it }
        }
        return null
    }



}