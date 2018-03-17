package com.ghostwan.robotkit.robot.pepper

import android.app.Activity
import android.content.Context
import android.util.Log
import com.aldebaran.qi.Future
import com.aldebaran.qi.Session
import com.aldebaran.qi.sdk.`object`.actuation.Actuation
import com.aldebaran.qi.sdk.`object`.context.RobotContext
import com.aldebaran.qi.sdk.`object`.conversation.*
import com.aldebaran.qi.sdk.core.FocusManager
import com.aldebaran.qi.sdk.core.SessionManager
import com.aldebaran.qi.sdk.util.IOUtils
import com.ghostwan.robotkit.robot.pepper.`object`.Concept
import com.ghostwan.robotkit.robot.pepper.`object`.Discussion
import com.ghostwan.robotkit.robot.pepper.ext.await
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
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
    var session: Session? = null
    var robotContext: RobotContext? = null
    var conversation: Conversation? = null
    var actuation: Actuation? = null
    private var onRobotLost: ((String) -> Unit)? = null

    companion object {
        val TAG = "MyPepper"
        fun info(message: String) {
            Log.i(TAG, message)
        }
        fun warning(message: String) {
            Log.w(TAG, message)
        }
        fun exception(e: Exception, message: String="error") {
            Log.e(TAG, message, e)
        }
        fun ui (block: suspend CoroutineScope.() -> Unit) {
            launch(UI, block = block)
        }
    }

    fun setOnRobotLost(function: (String) -> Unit) {
        onRobotLost = function
    }

    suspend fun connect() {
        robotContext = util.deserializeRobotContext(focusManager.await(onRobotLost))
        Log.i(TAG, "session connected")
        session = sessionManager.await(context, onRobotLost)
        Log.i(TAG, "focus retrieved")
        conversation = util.retrieveService(session!!, Conversation::class.java, "Conversation")
        actuation = util.retrieveService(session!!, Actuation::class.java, "Actuation")
        Log.i(TAG, "services retrieved")
    }

    fun isConnected(): Boolean = session != null && session!!.isConnected

    private suspend fun <T : Any?> handleFuture(future: Future<T>, onResult: ((Result<T>) -> Unit)?, throwOnCancel:Boolean): T? {
        futures.add(future)
        future.thenConsume {
            futures.remove(future)
            when {
                it.isSuccess -> onResult?.invoke(Success(it.value))
                it.isCancelled -> onResult?.invoke(Failure(CancellationException()))
                else -> onResult?.invoke(Failure(it.error))
            }
        }

        if(onResult == null) {
            try {
                return future.await()
            } catch (e : CancellationException){
                if(throwOnCancel)
                    throw e
            }
        }
        return null
    }

    fun stop() {
        Log.i(TAG, "cancelling ${futures.size} futures")
        for (future in futures) {
            future.requestCancellation();
            futures.remove(future)
        }
    }

    suspend fun say(phraseRes: Int, vararg animationsRes : Int,
                    bodyLanguageOption: BodyLanguageOption? =null,
                    onResult: ((Result<Void>) -> Unit)? = null,
                    throwOnCancel:Boolean = true) {

        say(context.getString(phraseRes), *animationsRes, bodyLanguageOption = bodyLanguageOption, onResult = onResult, throwOnCancel = throwOnCancel)
    }

    suspend fun say(phrase: String, vararg animationsRes : Int,
                    bodyLanguageOption: BodyLanguageOption? =null,
                    onResult: ((Result<Void>) -> Unit)? = null,
                    throwOnCancel:Boolean = true) {

        val say = if(bodyLanguageOption != null)
            conversation?.async()?.makeSay(robotContext, Phrase(phrase), bodyLanguageOption).await()
        else
            conversation?.async()?.makeSay(robotContext, Phrase(phrase)).await()

        val future = if(animationsRes.isNotEmpty()) {
            val animSet = ArrayList<String>()
            animationsRes.mapTo(animSet) { IOUtils.fromRaw(context, it)}

            val animation = actuation?.async()?.makeAnimation(animSet).await()
            val animate = actuation?.async()?.makeAnimate(robotContext, animation).await()

            Future.waitAll(animate.async().run(), say.async().run())
        }
        else {
            say.async().run()
        }

        handleFuture(future, onResult, throwOnCancel)
    }

    suspend fun listen(vararg concepts: Concept, bodyLanguageOption: BodyLanguageOption? =null,
                       throwOnCancel:Boolean = true): Concept? {
        val phraseSet = ArrayList<PhraseSet>()
        concepts.mapTo(phraseSet) { conversation?.async()?.makePhraseSet(it.phrases).await() }

        val listen = if(bodyLanguageOption != null)
            conversation?.async()?.makeListen(robotContext, phraseSet, bodyLanguageOption).await()
        else
            conversation?.async()?.makeListen(robotContext, phraseSet).await()

        val future = listen.async().run()
        val listenResult = handleFuture(future, null, throwOnCancel)

        listenResult?.heardPhrase.let {
            concepts
                    .filter { concept -> concept.isPhraseInConcept(it!!) }
                    .forEach { return it }
        }
        return null
    }

    suspend fun animate(mainAnimation:Int, vararg additionalAnimations: Int,
                        onResult: ((Result<Void>) -> Unit)? = null,
                        throwOnCancel:Boolean = true) {
        val animSet = ArrayList<String>()
        animSet.add(IOUtils.fromRaw(context, mainAnimation))
        additionalAnimations.mapTo(animSet) { IOUtils.fromRaw(context, it)}

        val animation = actuation?.async()?.makeAnimation(animSet).await()
        val animate = actuation?.async()?.makeAnimate(robotContext, animation).await()

        val future = animate.async().run()
        handleFuture(future, onResult, throwOnCancel)
    }

    suspend fun discuss(mainTopic : Int, vararg additionalTopics : Int,
                        onResult: ((Result<String>) -> Unit)? = null,
                        throwOnCancel:Boolean = true,
                        gotoBookmark : String? = null) : String? {
        val topicSet = ArrayList<Topic>()
        topicSet.add(conversation?.async()?.makeTopic(IOUtils.fromRaw(context, mainTopic)).await())
        additionalTopics.mapTo(topicSet) { conversation?.async()?.makeTopic(IOUtils.fromRaw(context, it)).await() }

        val discuss = conversation?.async()?.makeDiscuss(robotContext, topicSet).await()

        val future = discuss.async().run()
        gotoBookmark.let {
            discuss.setOnStartedListener {
                launch(UI) {
                    val bookmark = topicSet[0].async().bookmarks.await()[gotoBookmark]
                    discuss.async().goToBookmarkedOutputUtterance(bookmark).await()
                }
            }
        }
        return handleFuture(future, onResult, throwOnCancel)
    }

    suspend fun discuss(discussion: Discussion,
                        onResult: ((Result<String>) -> Unit)? = null,
                        throwOnCancel:Boolean = true,
                        gotoBookmark : String? = null) : String? {
        val topicSet = ArrayList<Topic>()
        discussion.topics.mapTo(topicSet) { conversation?.async()?.makeTopic(it).await() }

        val discuss = conversation?.async()?.makeDiscuss(robotContext, topicSet).await()
        var startBookmark:String? = null
        discussion.prepare(discuss)?.let {
            startBookmark = it
        }
        gotoBookmark?.let {
            startBookmark = gotoBookmark
        }
        info("Start bookmark : $startBookmark")
        startBookmark?.let {
            discuss?.async()?.setOnStartedListener {
                launch(UI) {
                    val bookmark = topicSet[0].async().bookmarks.await()[startBookmark]
                    discuss.async().goToBookmarkedOutputUtterance(bookmark).await()
                }
            }
        }
        val future = discuss.async().run()
        return handleFuture(future, onResult, throwOnCancel)
    }


}