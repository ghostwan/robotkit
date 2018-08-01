package com.ghostwan.robotkit.naoqi

import android.app.Activity
import android.os.Build
import android.provider.Settings
import android.support.annotation.RawRes
import android.support.annotation.StringRes
import com.aldebaran.qi.Future
import com.aldebaran.qi.Session
import com.aldebaran.qi.UserTokenAuthenticator
import com.aldebaran.qi.sdk.`object`.context.RobotContext
import com.aldebaran.qi.sdk.`object`.conversation.*
import com.aldebaran.qi.sdk.`object`.focus.FocusOwner
import com.aldebaran.qi.sdk.`object`.touch.TouchSensor
import com.ghostwan.robotkit.Robot
import com.ghostwan.robotkit.`object`.Action
import com.ghostwan.robotkit.`object`.Failure
import com.ghostwan.robotkit.`object`.Result
import com.ghostwan.robotkit.`object`.Success
import com.ghostwan.robotkit.ext.getLocalizedRaw
import com.ghostwan.robotkit.ext.getLocalizedString
import com.ghostwan.robotkit.ext.getRaw
import com.ghostwan.robotkit.naoqi.`object`.*
import com.ghostwan.robotkit.naoqi.ext.await
import com.ghostwan.robotkit.naoqi.ext.toNaoqiLocale
import com.ghostwan.robotkit.util.infoLog
import com.ghostwan.robotkit.util.ui
import com.ghostwan.robotkit.util.weakRef
import kotlinx.coroutines.experimental.CancellationException
import java.util.*

/**
 * Interface to call robotics API on Naoqi's robots
 */
abstract class NaoqiRobot(activity: Activity, private val address: String?, private val password: String?) : Robot{

    companion object {
        private const val USER_SESSION = "nao"
    }

    private var futures: MutableMap<Future<*>, Array<out Action>> = HashMap()
    protected var weakActivity: Activity by weakRef(activity)
    private val lock = Any()

    var robotContext: RobotContext? = null
        protected set
    var session: Session = Session()

    var services : NaoqiServices = NaoqiServices(session)


    private var focusOwner: FocusOwner? =null

    var robotLostListener: ((String) -> Unit)? = null
    var touchSensors = ArrayList<TouchSensor>()
    protected var bodyTouchedListener: ((BodyPart, TouchState) -> Unit)? = null


    private suspend fun <T : Any?> handleFuture(future: Future<T>,
                                                onResult: (suspend  (Result<T>) -> Unit)?,
                                                throwOnCancel: Boolean,
                                                vararg actions: Action): T? {
        synchronized(lock) {
            futures.put(future, actions)
        }
        future.thenConsume {
            synchronized(lock) {
                futures.remove(future)
            }
            ui {
                when {
                    it.isSuccess -> onResult?.invoke(Success(it.value))
                    it.isCancelled -> onResult?.invoke(Failure(CancellationException()))
                    else -> onResult?.invoke(Failure(it.error))
                }
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

    abstract fun getRobotType() : String

    override fun setOnRobotLost(function: (String) -> Unit) {
        robotLostListener = function
    }

    override suspend fun connect() {

        if (isConnected()) {
            disconnect()
        }
        session = Session()
        services = NaoqiServices(session)

        if(password != null) {
            session.setClientAuthenticator(UserTokenAuthenticator(USER_SESSION,password))
        }
        session.addConnectionListener(object: Session.ConnectionListener {
            override fun onConnected() {
            }

            override fun onDisconnected(reason: String?) {
                if(reason == null) {
                    robotLostListener?.invoke("")
                }else {
                    robotLostListener?.invoke(reason)
                }
            }

        })
        session.connect(address)?.await()
        infoLog("session connected")

        if(!hasFocus())
            takeFocus()
    }

    open suspend fun hasFocus() : Boolean {
        return if(focusOwner != null) {
            services.focus.await().async().check(focusOwner).await()
        } else {
            false
        }
    }

    /**
     * Get device locale
     */
    open fun getCurrentLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            weakActivity.resources.configuration.locales[0]
        } else {
            weakActivity.resources.configuration.locale
        }
    }


    suspend fun releaseFocus() {
        focusOwner?.async()?.release().await()
        infoLog("Focus Released!")
    }

    open suspend fun takeFocus() {
        takeFocus(null)
    }

    protected suspend fun takeFocus(token : String?) {
        val deviceId = Settings.Secure.getString(weakActivity.contentResolver, Settings.Secure.ANDROID_ID)
        val packageId = weakActivity.packageName
        focusOwner = if(token == null) {
            services.focus.await().async().take().await()
        }else {
            services.focus.await().async().take(token).await()
        }
        infoLog("Focus Granted")
        robotContext = services.contextFactory.await().async().makeContext().await()
        robotContext?.async()?.setFocus(focusOwner).await()
        robotContext?.async()?.setIdentity("$deviceId:$packageId").await()
        infoLog("Robot Context initialized")
    }

    override suspend fun disconnect() {
        if (isConnected()) {
            if(touchSensors.isNotEmpty())
                infoLog("Removing listeners")
            touchSensors.map {
                it.async().removeAllOnStateChangedListeners().await()
            }
            releaseFocus()
            infoLog("Closing session")
            session.close().await()
        }
        touchSensors.clear()
    }


    override fun isConnected(): Boolean = session.isConnected

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
                            future.requestCancellation();
                            futures.remove(future)
                            break@loop
                        }
                    }
                }
            }
        }
    }

    override fun stopAllBut(vararg actions: Action) {
        synchronized(lock) {
            if (actions.isEmpty()) {
                stop()
            }
            else {
                loop@ for ((future, futureActions) in futures) {
                    for(action in actions) {
                        if (action !in futureActions) {
                            future.requestCancellation();
                            futures.remove(future)
                            break@loop
                        }
                    }
                }
            }
        }
    }

    private suspend fun connectSensors(){
        for (sensorName in services.touch.await().async().sensorNames.await()) {
            val touchSensor = services.touch.await().async().getSensor(sensorName).await()
            touchSensor?.let {
                it.async().addOnStateChangedListener {
                    ui {
                        val state = if (it.touched) {
                            TouchState.TOUCHED
                        } else {
                            TouchState.RELEASED
                        }

                        when (sensorName) {
                            "Head/Touch" -> bodyTouchedListener?.invoke(BodyPart.HEAD, state)
                            "LHand/Touch" -> bodyTouchedListener?.invoke(BodyPart.LEFT_HAND, state)
                            "RHand/Touch" -> bodyTouchedListener?.invoke(BodyPart.RIGHT_HAND, state)
                            "Bumper/FrontLeft" -> bodyTouchedListener?.invoke(BodyPart.LEFT_BUMPER, state)
                            "Bumper/FrontRight" -> bodyTouchedListener?.invoke(BodyPart.RIGHT_BUMPER, state)
                            "Bumper/Back" -> bodyTouchedListener?.invoke(BodyPart.HEAD, state)
                        }
                    }
                }.await()
                touchSensors.add(it)
            }
        }
        infoLog("sensors connected")
    }


    /**
     * Make the robot say a phrase using a string resource and optionally play an animation at the same time
     *
     * If [onResult] it's not set, the coroutine will be suspended until the robot finish its speech without blocking the thread
     *
     * @param phraseRes The phrase to say. It is an Android string resource.
     *
     * @param animationsRes Animations which will be played during the speech. They are android raw resources.
     *
     * @param bodyLanguageOption Body language policy
     *
     * @param locale The locale use to say something, if it's null use the one of the device.
     *
     * @param throwOnStop By default it's true. Does this method throw when the stop api is called and stop it.
     * Could be useful to set it to false when we want to continue actions scenario after a stop
     *
     * @param onStart This lambada is called when the action start on the robot
     *
     * @param onResult If this lambada is set the coroutine won't wait and the result of
     * the api call will be forward to the lambda.
     * Give a [Success] or a [Failure] with the exception
     *
     */
    suspend fun say(@StringRes phraseRes: Int, @RawRes vararg animationsRes: Int, bodyLanguageOption: BodyLanguageOption = BodyLanguageOption.NEUTRAL, locale : Locale?=null,
                    throwOnStop: Boolean = true,
                    onStart: (suspend () -> Unit)? = null,
                    onResult: (suspend (Result<Void>) -> Unit)? = null) {

        val string = weakActivity.getLocalizedString(phraseRes, locale)

        say(string, *animationsRes, bodyLanguageOption = bodyLanguageOption, locale = locale,
                throwOnStop = throwOnStop,
                onStart = onStart,
                onResult = onResult)
    }

    /**
     * Make the robot say a phrase and optionally play an animation at the same time
     *
     * If [onResult] it's not set, the coroutine will be suspended until the robot finish its speech without blocking the thread
     *
     * @param phrase The phrase to say.
     *
     * @param animationsRes Animations which will be played during the speech. They are android raw resources.
     *
     * @param bodyLanguageOption Body language policy
     *
     * @param locale The locale use to say something, if it's null use the one of the device.
     *
     * @param throwOnStop By default it's true. Does this method throw when the stop api is called and stop it.
     * Could be useful to set it to false when we want to continue actions scenario after a stop
     *
     * @param onStart This lambada is called when the action start on the robot
     *
     * @param onResult If this lambada is set the coroutine won't wait and the result of
     * the api call will be forward to the lambda.
     * Give a [Success] or a [Failure] with the exception
     *
     */
    suspend fun say(phrase: String, @RawRes vararg animationsRes: Int, bodyLanguageOption: BodyLanguageOption = BodyLanguageOption.NEUTRAL, locale : Locale?=null,
                    throwOnStop: Boolean = true,
                    onStart: (suspend () -> Unit)? = null,
                    onResult: (suspend (Result<Void>) -> Unit)? = null){

        val say = if (locale == null) {
            services.conversation.await().async()?.makeSay(robotContext, Phrase(phrase), bodyLanguageOption, getCurrentLocale().toNaoqiLocale()).await()
        } else {
            services.conversation.await().async()?.makeSay(robotContext, Phrase(phrase), bodyLanguageOption, locale.toNaoqiLocale()).await()
        }

        onStart?.let {
            say.async().addOnStartedListener { ui { it() } }.await()
        }

        val future = if (animationsRes.isNotEmpty()) {
            val animSet = ArrayList<String>()
            animationsRes.mapTo(animSet) { weakActivity.getLocalizedRaw(it, locale) }

            val animation = services.actuation.await().async().makeAnimation(animSet).await()
            val animate = services.actuation.await().async().makeAnimate(robotContext, animation).await()

            Future.waitAll(animate.async().run(), say.async().run())
        } else {
            say.async().run()
        }
        if (animationsRes.isEmpty())
            handleFuture(future, onResult, throwOnStop, Action.TALKING)
        else
            handleFuture(future, onResult, throwOnStop, Action.TALKING, Action.MOVING)
    }

    /**
     * Make the robot listen concepts, where a concept is a group of phrase we want to match
     *
     * If [onResult] it's not set, the coroutine will be suspended until the robot match something, without blocking the thread
     *
     * @param concepts Set of concept we want to recognize
     *
     * @param bodyLanguageOption Body language policy
     *
     * @param locale The locale use to listen sentence, if it's null use the one of the device.
     *
     * @param throwOnStop By default it's true. Does this method throw when the stop api is called and stop it.
     * Could be useful to set it to false when we want to continue actions scenario after a stop
     *
     * @param onStart This lambada is called when the action start on the robot
     *
     * @param onResult If this lambada is set the coroutine won't wait and the result of
     * the api call will be forward to the lambda.
     * Give a [Success] containing the concept that matched or a [Failure] with the exception
     *
     * @return the concept that matched if [onResult] it's not set
     *
     */
    suspend fun listen(vararg concepts: Concept, bodyLanguageOption: BodyLanguageOption? = BodyLanguageOption.NEUTRAL, locale : Locale?=null,
                       throwOnStop: Boolean = true,
                       onStart: (suspend () -> Unit)? = null,
                       onResult: (suspend (Result<Concept>) -> Unit)? = null
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
            services.conversation.await().async().makePhraseSet(phrases).await()
        }

        val listen = if (locale == null) {
            services.conversation.await().async().makeListen(robotContext, phraseSet, bodyLanguageOption, getCurrentLocale().toNaoqiLocale()).await()
        } else {
            services.conversation.await().async().makeListen(robotContext, phraseSet, bodyLanguageOption, locale.toNaoqiLocale()).await()
        }

        onStart?.let {
            listen.async().addOnStartedListener { ui { it() } }.await()
        }

        var onResultListen: ( suspend (Result<ListenResult>) -> Unit)? = null
        onResult?.let {
            onResultListen = {
                when (it) {
                    is Success -> it.value.heardPhrase.let {
                        concepts.filter { concept ->
                            when (concept) {
                                is StrConcept -> concept.isPhraseInConcept(it.text)
                                is ResConcept -> concept.isPhraseInConcept(stringToRes[it.text]!!)
                            }
                        }.map { onResult(Success(it)) }
                    }
                    is Failure -> onResult(Failure(CancellationException()))
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

    /**
     * Animate the robot with an animation using raw qianim file
     *
     * If [onResult] it's not set, the coroutine will be suspended until the animation end, without blocking the thread
     *
     * @param mainAnimation Main animation. It's an android raw resources.
     *
     * @param additionalAnimations Additional animation as for example trajectory to make the robot dance.
     * They are android raw resources.
     *
     * @param throwOnStop By default it's true. Does this method throw when the stop api is called and stop it.
     * Could be useful to set it to false when we want to continue actions scenario after a stop
     *
     * @param onStart This lambada is called when the action start on the robot
     *
     * @param onResult If this lambada is set the coroutine won't wait and the result of
     * the api call will be forward to the lambda.
     * Give a [Success] or a [Failure] with the exception
     *
     */
    suspend fun animate(@RawRes mainAnimation: Int, @RawRes vararg additionalAnimations: Int,
                        throwOnStop: Boolean = true,
                        onStart: (suspend () -> Unit)? = null,
                        onResult: (suspend (Result<Void>) -> Unit)? = null) {

        val animSet = ArrayList<String>()
        animSet.add(weakActivity.getRaw(mainAnimation))
        additionalAnimations.mapTo(animSet) { weakActivity.getRaw(it) }

        val animation = services.actuation.await().async().makeAnimation(animSet).await()
        val animate = services.actuation.await().async().makeAnimate(robotContext, animation).await()

        onStart?.let {
            animate.async().addOnStartedListener { ui { it() } }.await()
        }


        val future = animate.async().run()
        handleFuture(future, onResult, throwOnStop, Action.MOVING)
    }

    /**
     * Make the robot discuss using raw qichat topics
     *
     * If [onResult] it's not set, the coroutine will be suspended until the discussion end, without blocking the thread
     *
     * @param mainTopic Main topic for the discussion. It's an android raw resources.
     * The gotoBookmark will use this topic.
     *
     * @param additionalTopics Additional topics for the discussion. They are android raw resources.
     * It could be useful if a discussion is composed of different topics or if concept are define in
     * other files
     *
     * @param gotoBookmark Name of the bookmark in the main topic to go to. If it not set the discussion
     * will wait for a rules to match
     *
     * @param locale The locale use to discuss about something, if it's null use the one of the device.
     *
     * @param throwOnStop By default it's true. Does this method throw when the stop api is called and stop it.
     * Could be useful to set it to false when we want to continue actions scenario after a stop
     *
     * @param onStart This lambada is called when the action start on the robot
     *
     * @param onResult If this lambada is set the coroutine won't wait and the result of
     * the api call will be forward to the lambda.
     * Give a [Success] containing the result of the discussion or a [Failure] with the exception
     *
     * @return the result of the discussion if [onResult] it's not set
     *
     */
    suspend fun discuss(@RawRes mainTopic: Int, @RawRes vararg additionalTopics: Int, gotoBookmark: String? = null, locale : Locale?=null,
                        throwOnStop: Boolean = true,
                        onStart: (suspend () -> Unit)? = null,
                        onResult: (suspend (Result<String>) -> Unit)? = null
    ): String? {

        val discussion = Discussion(weakActivity, mainTopic, *additionalTopics, locale = locale)
        return discuss(discussion, gotoBookmark, throwOnStop, onStart, onResult)
    }

    /**
     * Make the robot discuss using a Discussion object made of qichat topics
     *
     * If [onResult] it's not set, the coroutine will be suspended until the discussion end, without blocking the thread
     *
     * @param discussion Object that handle the discussion
     *
     * @param gotoBookmark Name of the bookmark in the main topic to go to. If it not set the discussion
     * will wait for a rules to match
     *
     * @param throwOnStop By default it's true. Does this method throw when the stop api is called and stop it.
     * Could be useful to set it to false when we want to continue actions scenario after a stop
     *
     * @param onStart This lambada is called when the action start on the robot
     *
     * @param onResult If this lambada is set the coroutine won't wait and the result of
     * the api call will be forward to the lambda.
     * Give a [Success] containing the result of the discussion or a [Failure] with the exception
     *
     * @return the result of the discussion if [onResult] it's not set
     *
     */
    suspend fun discuss(discussion: Discussion, gotoBookmark: String? = null,
                        throwOnStop: Boolean = true,
                        onStart: (suspend () -> Unit)? = null,
                        onResult: (suspend (Result<String>) -> Unit)? = null
    ): String?{

        val topics = discussion.topics.map { (key, value) ->
            val top = services.conversation.await().async()?.makeTopic(value).await()
            key to top
        }.toMap()

        val qichatbot = if (discussion.locale == null) {
            services.conversation.await().async()?.makeQiChatbot(robotContext, topics.values.toList(), getCurrentLocale().toNaoqiLocale()).await()
        } else {
            services.conversation.await().async()?.makeQiChatbot(robotContext, topics.values.toList(), discussion.locale.toNaoqiLocale()).await()
        }
        val chatbots = listOf<Chatbot>(qichatbot)

        val chat = if (discussion.locale == null) {
            services.conversation.await().async()?.makeChat(robotContext, chatbots, getCurrentLocale().toNaoqiLocale()).await()
        } else {
            services.conversation.await().async()?.makeChat(robotContext, chatbots, discussion.locale.toNaoqiLocale()).await()
        }

        var startBookmark: String? = null
        discussion.prepare(qichatbot, topics)?.let {
            startBookmark = it
        }
        gotoBookmark?.let {
            startBookmark = gotoBookmark
        }
        chat.async().addOnStartedListener {
            ui {
                onStart?.invoke()
                if (startBookmark != null) {
                    val bookmark = topics[discussion.mainTopic]?.async()?.bookmarks.await()[startBookmark]
                    qichatbot.async().goToBookmark(bookmark, AutonomousReactionImportance.HIGH, AutonomousReactionValidity.IMMEDIATE).await()
                }
            }
        }.await()

        var endValue : String? = null
        val futureChat = chat.async().run()
        qichatbot.async().addOnEndedListener {
            endValue = it
            if(chatbots.contains(qichatbot) && chatbots.size <= 1)
                futureChat.requestCancellation()
        }.await()
        val future = futureChat.thenApply { endValue ?: "" }
        return handleFuture(future, onResult, throwOnStop, Action.TALKING, Action.LISTENING, Action.DISCUSSING)
    }

    /**
     * Set the callback called when Pepper's body is touched
     *
     * @param function lambda called with the [BodyPart] touched and the [TouchState]
     */
    suspend fun setOnBodyTouched(function: (BodyPart, TouchState) -> Unit) {
        bodyTouchedListener = function
        if (touchSensors.isEmpty() && isConnected()) {
            connectSensors()
        }
    }


}