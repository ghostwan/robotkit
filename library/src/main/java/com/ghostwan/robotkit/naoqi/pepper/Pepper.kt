package com.ghostwan.robotkit.naoqi.pepper

import android.app.Activity
import com.aldebaran.qi.Future
import com.aldebaran.qi.Session
import com.aldebaran.qi.sdk.`object`.actuation.Actuation
import com.aldebaran.qi.sdk.`object`.context.RobotContext
import com.aldebaran.qi.sdk.`object`.conversation.*
import com.ghostwan.robotkit.`object`.Action
import com.ghostwan.robotkit.`object`.Failure
import com.ghostwan.robotkit.`object`.Result
import com.ghostwan.robotkit.`object`.Success
import com.ghostwan.robotkit.naoqi.NaoqiRobot
import com.ghostwan.robotkit.naoqi.`object`.*
import com.ghostwan.robotkit.naoqi.ext.*
import com.ghostwan.robotkit.util.info
import com.ghostwan.robotkit.util.ui
import com.ghostwan.robotkit.util.weakRef
import kotlinx.coroutines.experimental.CancellationException
import java.util.*

abstract class Pepper(activity: Activity) : NaoqiRobot {

    protected var futures: MutableMap<Future<*>, Array<out Action>> = HashMap()
    protected var util: PepperUtil = PepperUtil()
    protected var weakActivity: Activity by weakRef(activity)
    protected val lock = Any()

    var session: Session? = null
    var robotContext: RobotContext? = null

    // Naoqi services
    var conversation: Conversation? = null
    var actuation: Actuation? = null

    var robotLostListener: ((String) -> Unit)? = null

    override fun setOnRobotLost(function: (String) -> Unit) {
        robotLostListener = function
    }

    private suspend fun <T : Any?> handleFuture(future: Future<T>,
                                                onResult: ((Result<T>) -> Unit)?,
                                                throwOnCancel: Boolean,
                                                vararg actions: Action): T? {
        synchronized(lock) {
            futures.put(future, actions)
        }
        future.thenConsume {
            synchronized(lock) {
                futures.remove(future)
            }
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

    override fun isConnected(): Boolean = session != null && session!!.isConnected

    override fun stop(vararg actions: Action) {
        synchronized(lock) {
            if (actions.isEmpty()) {
                for ((future, _) in futures) {
                    future.requestCancellation();
                    futures.remove(future)
                }
            }
            else {
                loop@ for ((future, futureActions) in futures) {
                    for(action in actions) {
                        if (action in futureActions) {
//                            warning("Stopping action $action")
                            future.requestCancellation();
                            futures.remove(future)
                            break@loop
                        }
                    }
                }
            }
        }
    }

    override suspend fun say(phraseRes: Int, vararg animationsRes: Int,
                             bodyLanguageOption: BodyLanguageOption, locale: Locale?,
                             throwOnStop: Boolean,
                             onStart: (() -> Unit)?,
                             onResult: ((Result<Void>) -> Unit)?) {

        val string = weakActivity.getLocalizedString(phraseRes, locale)

        say(string, *animationsRes, bodyLanguageOption = bodyLanguageOption, locale = locale,
                throwOnStop = throwOnStop,
                onStart = onStart,
                onResult = onResult)
    }

    override suspend fun say(phrase: String, vararg animationsRes: Int,
                             bodyLanguageOption: BodyLanguageOption, locale: Locale?,
                             throwOnStop: Boolean,
                             onStart: (() -> Unit)?,
                             onResult: ((Result<Void>) -> Unit)?) {

        val say = if (locale == null) {
            conversation?.async()?.makeSay(robotContext, Phrase(phrase), bodyLanguageOption).await()
        } else {
            conversation?.async()?.makeSay(robotContext, Phrase(phrase), bodyLanguageOption, locale.toNaoqiLocale()).await()
        }

        say.async().setOnStartedListener(onStart)

        val future = if (animationsRes.isNotEmpty()) {
            val animSet = ArrayList<String>()
            animationsRes.mapTo(animSet) { weakActivity.getLocalizedRaw(it, locale) }

            val animation = actuation?.async()?.makeAnimation(animSet).await()
            val animate = actuation?.async()?.makeAnimate(robotContext, animation).await()

            Future.waitAll(animate.async().run(), say.async().run())
        } else {
            say.async().run()
        }
        if (animationsRes.isEmpty())
            handleFuture(future, onResult, throwOnStop, Action.TALKING)
        else
            handleFuture(future, onResult, throwOnStop, Action.TALKING, Action.MOVING)
    }

    override suspend fun listen(vararg concepts: Concept, bodyLanguageOption: BodyLanguageOption?, locale: Locale?,
                                throwOnStop: Boolean,
                                onStart: (() -> Unit)?,
                                onResult: ((Result<Concept>) -> Unit)?
    ): Concept? {

        val stringToRes: MutableMap<String, Int> = mutableMapOf()

        val phraseSet = concepts.map {
            val phrases: List<Phrase> = when (it) {
                is StrConcept -> it.phrases.map { Phrase(it) }
                is ResConcept -> it.phrases.map {
                    val string = weakActivity.getLocalizedString(it, locale)
                    stringToRes[string] = it
                    Phrase(string)
                }
            }
            conversation?.async()?.makePhraseSet(phrases).await()
        }

        val listen = if (locale == null) {
            conversation?.async()?.makeListen(robotContext, phraseSet, bodyLanguageOption).await()
        } else {
            conversation?.async()?.makeListen(robotContext, phraseSet, bodyLanguageOption, locale.toNaoqiLocale()).await()
        }

        listen.async().setOnStartedListener(onStart)

        var onResultListen: ((Result<ListenResult>) -> Unit)? = null
        if (onResult != null) {
            onResultListen = {
                when (it) {
                    is Success -> it.value.heardPhrase.let {
                        concepts.filter { concept ->
                            when (concept) {
                                is StrConcept -> concept.isPhraseInConcept(it.text)
                                is ResConcept -> concept.isPhraseInConcept(stringToRes[it.text]!!)
                            }
                        }.map { onResult.invoke(Success(it)) }
                    }
                    is Failure -> onResult.invoke(Failure(CancellationException()))
                }
            }
        }

        val future = listen.async().run()
        val listenResult = handleFuture(future, onResultListen, throwOnStop, Action.LISTENING)

        listenResult?.heardPhrase?.let {
            concepts.filter { concept ->
                when (concept) {
                    is StrConcept -> concept.isPhraseInConcept(it.text)
                    is ResConcept -> concept.isPhraseInConcept(stringToRes[it.text]!!)
                }
            }.map { return it }
        }
        return null
    }

    override suspend fun animate(mainAnimation: Int, vararg additionalAnimations: Int,
                                 throwOnStop: Boolean,
                                 onStart: (() -> Unit)?,
                                 onResult: ((Result<Void>) -> Unit)?) {

        val animSet = ArrayList<String>()
        animSet.add(weakActivity.getRaw(mainAnimation))
        additionalAnimations.mapTo(animSet) { weakActivity.getRaw(it) }

        val animation = actuation?.async()?.makeAnimation(animSet).await()
        val animate = actuation?.async()?.makeAnimate(robotContext, animation).await()
        animate.async().setOnStartedListener(onStart)

        val future = animate.async().run()
        handleFuture(future, onResult, throwOnStop, Action.MOVING)
    }

    override suspend fun discuss(mainTopic: Int, vararg additionalTopics: Int, gotoBookmark: String?, locale: Locale?,
                                 throwOnStop: Boolean,
                                 onStart: (() -> Unit)?,
                                 onResult: ((Result<String>) -> Unit)?
    ): String? {

        val topicSet = ArrayList<Topic>()
        topicSet.add(conversation?.async()?.makeTopic(weakActivity.getLocalizedRaw(mainTopic, locale)).await())
        additionalTopics.mapTo(topicSet) { conversation?.async()?.makeTopic(weakActivity.getLocalizedRaw(it, locale)).await() }

        val discuss = if (locale == null) {
            conversation?.async()?.makeDiscuss(robotContext, topicSet).await()
        } else {
            conversation?.async()?.makeDiscuss(robotContext, topicSet, locale.toNaoqiLocale()).await()
        }

        val future = discuss.async().run()
        discuss.setOnStartedListener {
            ui {
                onStart?.invoke()
                if (gotoBookmark != null) {
                    info("Go to bookmark : $gotoBookmark")
                    val bookmark = topicSet[0].async().bookmarks.await()[gotoBookmark]
                    discuss.async().goToBookmarkedOutputUtterance(bookmark).await()
                }
            }
        }
        return handleFuture(future, onResult, throwOnStop, Action.TALKING, Action.LISTENING)
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

        val discuss = if (discussion.locale == null) {
            conversation?.async()?.makeDiscuss(robotContext, topics.values.toList()).await()
        } else {
            conversation?.async()?.makeDiscuss(robotContext, topics.values.toList(), discussion.locale.toNaoqiLocale()).await()
        }

        var startBookmark: String? = null
        discussion.prepare(discuss, topics)?.let {
            startBookmark = it
        }
        gotoBookmark?.let {
            startBookmark = gotoBookmark
        }
        discuss.setOnStartedListener {
            ui {
                onStart?.invoke()
                if (startBookmark != null) {
                    info("Go to bookmark : $startBookmark")
                    val bookmark = topics[discussion.mainTopic]?.async()?.bookmarks.await()[startBookmark]
                    discuss.async().goToBookmarkedOutputUtterance(bookmark).await()
                }
            }
        }
        val future = discuss.async().run()
        return handleFuture(future, onResult, throwOnStop, Action.TALKING, Action.LISTENING)
    }

}