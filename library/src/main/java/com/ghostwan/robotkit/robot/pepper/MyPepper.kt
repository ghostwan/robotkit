package com.ghostwan.robotkit.robot.pepper

import android.app.Activity
import android.util.Log
import com.aldebaran.qi.Future
import com.aldebaran.qi.Session
import com.aldebaran.qi.sdk.`object`.actuation.Actuation
import com.aldebaran.qi.sdk.`object`.context.RobotContext
import com.aldebaran.qi.sdk.`object`.conversation.*
import com.aldebaran.qi.sdk.core.FocusManager
import com.aldebaran.qi.sdk.core.SessionManager
import com.aldebaran.qi.sdk.util.IOUtils
import com.ghostwan.robotkit.robot.pepper.`object`.*
import com.ghostwan.robotkit.robot.pepper.ext.await
import com.ghostwan.robotkit.robot.pepper.util.PepperUtil
import com.ghostwan.robotkit.robot.pepper.util.ui
import com.ghostwan.robotkit.robot.pepper.util.weakRef
import java.util.concurrent.CancellationException


/**
 * Created by erwan on 10/03/2018.
 */
class MyPepper(activity: Activity) : Pepper {

    private var futures: MutableList<Future<*>> = ArrayList()
    private var util: PepperUtil = PepperUtil()
    private var weakActivity : Activity by weakRef(activity)
    private var focusManager: FocusManager? = null

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

        fun exception(t: Throwable?, message: String? = "error") {
            Log.e(TAG, message, t)
        }

    }

    private suspend fun <T : Any?> handleFuture(future: Future<T>, onResult: ((Result<T>) -> Unit)?, throwOnCancel: Boolean): T? {
        futures.add(future)
        future.thenConsume {
            futures.remove(future)
            when {
                it.isSuccess -> onResult?.invoke(Success(it.value))
                it.isCancelled -> onResult?.invoke(Failure(CancellationException()))
                else -> onResult?.invoke(Failure(it.error))
            }
        }

        if (onResult == null) {
            try {
                return future.await()
            } catch (e: CancellationException) {
                if (throwOnCancel)
                    throw e
            }
        }
        return null
    }

////////////////////////////////////////////////// OVERRIDE ////////////////////////////////////////////////////////////

    override fun setOnRobotLost(function: (String) -> Unit) {
        onRobotLost = function
    }

    override suspend fun connect() {

        if(isConnected())
            disconnect()

        var sessionManager = SessionManager(false)
        focusManager = FocusManager(weakActivity, sessionManager)

        val robotContextAO = focusManager!!.await(onRobotLost)
        robotContext = util.deserializeRobotContext(robotContextAO)
        Log.i(TAG, "session connected")
        session = weakActivity?.let {
            sessionManager!!.await(it, {
                focusManager?.unregister()
                onRobotLost?.invoke(it)
            })
        }
        Log.i(TAG, "focus retrieved")
        conversation = util.retrieveService(session!!, Conversation::class.java, "Conversation")
        actuation = util.retrieveService(session!!, Actuation::class.java, "Actuation")
        Log.i(TAG, "services retrieved")
    }

    override fun isConnected(): Boolean = session != null && session!!.isConnected

    override suspend fun disconnect() {
        if(isConnected()) {
            focusManager?.unregister()
            session?.close()
        }
    }

    override fun stop() {
        Log.i(TAG, "cancelling ${futures.size} futures")
        for (future in futures) {
            future.requestCancellation();
            futures.remove(future)
        }
    }

    override suspend fun say(phraseRes: Int, vararg animationsRes: Int, bodyLanguageOption: BodyLanguageOption?,
                             throwOnStop: Boolean,
                             onStart: (() -> Unit)?,
                             onResult: ((Result<Void>) -> Unit)?) {

        say(weakActivity!!.getString(phraseRes), *animationsRes, bodyLanguageOption = bodyLanguageOption,
                throwOnStop = throwOnStop,
                onStart = onStart,
                onResult = onResult)
    }

    override suspend fun say(phrase: String, vararg animationsRes: Int, bodyLanguageOption: BodyLanguageOption?,
                             throwOnStop: Boolean,
                             onStart: (() -> Unit)?,
                             onResult: ((Result<Void>) -> Unit)?) {

        val say = if (bodyLanguageOption != null)
            conversation?.async()?.makeSay(robotContext, Phrase(phrase), bodyLanguageOption).await()
        else
            conversation?.async()?.makeSay(robotContext, Phrase(phrase)).await()

        say.async().setOnStartedListener (onStart)

        val future = if (animationsRes.isNotEmpty()) {
            val animSet = ArrayList<String>()
            animationsRes.mapTo(animSet) { IOUtils.fromRaw(weakActivity, it) }

            val animation = actuation?.async()?.makeAnimation(animSet).await()
            val animate = actuation?.async()?.makeAnimate(robotContext, animation).await()

            Future.waitAll(animate.async().run(), say.async().run())
        } else {
            say.async().run()
        }

        handleFuture(future, onResult, throwOnStop)
    }

    override suspend fun listen(vararg concepts: Concept, bodyLanguageOption: BodyLanguageOption?,
                                throwOnStop: Boolean,
                                onStart: (() -> Unit)?,
                                onResult: ((Result<Concept>) -> Unit)?
    ): Concept? {

        val phraseSet = ArrayList<PhraseSet>()
        concepts.mapTo(phraseSet) { conversation?.async()?.makePhraseSet(it.phrases).await() }

        val listen = if (bodyLanguageOption != null)
            conversation?.async()?.makeListen(robotContext, phraseSet, bodyLanguageOption).await()
        else
            conversation?.async()?.makeListen(robotContext, phraseSet).await()

        listen.async().setOnStartedListener(onStart)

        var onResultListen : ((Result<ListenResult>) -> Unit)?= null
        if(onResult != null) {
             onResultListen = {
                 when(it){
                     is Success -> it.value.heardPhrase.let {
                         concepts.filter { concept -> concept.isPhraseInConcept(it!!) }.map { onResult.invoke(Success(it)) }
                     }
                     is Failure -> onResult.invoke(Failure(CancellationException()))
                 }
             }
        }

        val future = listen.async().run()
        val listenResult = handleFuture(future, onResultListen, throwOnStop)

        listenResult?.heardPhrase.let {
            concepts.filter { concept -> concept.isPhraseInConcept(it!!) }.map { return it }
        }
        return null
    }

    override suspend fun animate(mainAnimation: Int, vararg additionalAnimations: Int,
                                 throwOnStop: Boolean,
                                 onStart: (() -> Unit)?,
                                 onResult: ((Result<Void>) -> Unit)?) {

        val animSet = ArrayList<String>()
        animSet.add(IOUtils.fromRaw(weakActivity, mainAnimation))
        additionalAnimations.mapTo(animSet) { IOUtils.fromRaw(weakActivity, it) }

        val animation = actuation?.async()?.makeAnimation(animSet).await()
        val animate = actuation?.async()?.makeAnimate(robotContext, animation).await()
        animate.async().setOnStartedListener(onStart)

        val future = animate.async().run()
        handleFuture(future, onResult, throwOnStop)
    }

    override suspend fun discuss(mainTopic: Int, vararg additionalTopics: Int, gotoBookmark: String?,
                                 throwOnStop: Boolean,
                                 onStart: (() -> Unit)?,
                                 onResult: ((Result<String>) -> Unit)?
    ): String? {

        val topicSet = ArrayList<Topic>()
        topicSet.add(conversation?.async()?.makeTopic(IOUtils.fromRaw(weakActivity, mainTopic)).await())
        additionalTopics.mapTo(topicSet) { conversation?.async()?.makeTopic(IOUtils.fromRaw(weakActivity, it)).await() }

        val discuss = conversation?.async()?.makeDiscuss(robotContext, topicSet).await()

        val future = discuss.async().run()
        gotoBookmark.let {
            discuss.setOnStartedListener {
                ui {
                    val bookmark = topicSet[0].async().bookmarks.await()[gotoBookmark]
                    discuss.async().goToBookmarkedOutputUtterance(bookmark).await()
                    onStart?.invoke()
                }
            }
        }
        return handleFuture(future, onResult, throwOnStop)
    }

    override suspend fun discuss(discussion: Discussion, gotoBookmark: String?,
                                 throwOnStop: Boolean,
                                 onStart: (() -> Unit)?,
                                 onResult: ((Result<String>) -> Unit)?
    ): String? {

        val topics = discussion.topics.map { (key, value) ->
            val top = conversation?.async()?.makeTopic(value).await()
            key to top
        }.toMap()

        val discuss = conversation?.async()?.makeDiscuss(robotContext, topics.values.toList()).await()
        var startBookmark: String? = null
        discussion.prepare(discuss, topics)?.let {
            startBookmark = it
        }
        gotoBookmark?.let {
            startBookmark = gotoBookmark
        }
        info("Start bookmark : $startBookmark")
        startBookmark?.let {
            discuss?.async()?.setOnStartedListener {
                ui {
                    val bookmark = topics[discussion.mainTopic]?.async()?.bookmarks.await()[startBookmark]
                    discuss.async().goToBookmarkedOutputUtterance(bookmark).await()
                    onStart?.invoke()
                }
            }
        }
        val future = discuss.async().run()
        return handleFuture(future, onResult, throwOnStop)
    }


}