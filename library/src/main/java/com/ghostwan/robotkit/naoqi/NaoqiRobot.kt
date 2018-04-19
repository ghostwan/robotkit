package com.ghostwan.robotkit.naoqi

import android.app.Activity
import android.os.Build
import android.provider.Settings
import android.support.annotation.RawRes
import android.support.annotation.StringRes
import com.aldebaran.qi.AnyObject
import com.aldebaran.qi.Future
import com.aldebaran.qi.Session
import com.aldebaran.qi.sdk.`object`.context.RobotContext
import com.aldebaran.qi.sdk.`object`.conversation.BodyLanguageOption
import com.aldebaran.qi.sdk.`object`.conversation.ListenResult
import com.aldebaran.qi.sdk.`object`.conversation.Phrase
import com.aldebaran.qi.sdk.`object`.conversation.Topic
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
import com.ghostwan.robotkit.util.info
import com.ghostwan.robotkit.util.ui
import com.ghostwan.robotkit.util.weakRef
import kotlinx.coroutines.experimental.CancellationException
import java.util.*

/**
 * Interface to call robotics API on Naoqi's robots
 */
abstract class NaoqiRobot(activity: Activity, private val address: String?) : Robot{

    private var futures: MutableMap<Future<*>, Array<out Action>> = HashMap()
    protected var weakActivity: Activity by weakRef(activity)
    private val lock = Any()

    var robotContext: RobotContext? = null
        private set

    var services : NaoqiServices = NaoqiServices()
        private set

    var session: Session? = null
        private set

    var robotLostListener: ((String) -> Unit)? = null
    var touchSensors = ArrayList<TouchSensor>();
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

        val session = Session()
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

        initNaoqiData(session)
        takeFocus()
    }

    suspend fun isFocusOwn() : Boolean {
        return robotContext?.async()?.focus?.await()?.async()?.token().await() != null
    }

    suspend fun takeFocus() {
        val deviceId = Settings.Secure.getString(weakActivity.contentResolver, Settings.Secure.ANDROID_ID)
        val packageId = weakActivity.packageName
        val focusOwner = services.focus.async().take().await()
        robotContext = services.contextFactory.async().makeContext().await()
        robotContext?.async()?.setFocus(focusOwner).await()
        robotContext?.async()?.setIdentity("$deviceId:$packageId").await()
    }

    protected suspend fun initNaoqiData(session: Session, robotContextAO: AnyObject?=null){
        this.session = session
        services.retrieve(session)

        this.robotContext = robotContextAO?.let { services.deserializeRobotContext(robotContextAO) }
        if (bodyTouchedListener != null && touchSensors.isEmpty()) {
            connectSensors()
        }
    }

    override suspend fun disconnect() {
        if (isConnected()) {
            touchSensors.map {
                it.async().setOnStateChangedListener(null)//FIXME QiSDK 1.1.15 .await()
            }
            releaseFocus()
            session?.close()//FIXME QiSDK 1.1.15 .await()
        }
        touchSensors.clear()
    }

    suspend fun releaseFocus() {
        robotContext?.async()?.focus.await().async().release().await()
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
        for (sensorName in services.touch.async().sensorNames.await()) {
            val touchSensor = services.touch.async()?.getSensor(sensorName).await()
            touchSensor?.let {
                it.async().setOnStateChangedListener {
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
                }//FIXME QiSDK 1.1.15 .await()
                touchSensors.add(it)
            }
        }
        info("sensors connected")
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
            services.conversation.async()?.makeSay(robotContext, Phrase(phrase), bodyLanguageOption, getCurrentLocale().toNaoqiLocale()).await()
        } else {
            services.conversation.async()?.makeSay(robotContext, Phrase(phrase), bodyLanguageOption, locale.toNaoqiLocale()).await()
        }

        onStart?.let {
            say.async().setOnStartedListener { ui { it() } }//FIXME QiSDK 1.1.15 .await()
        }

        val future = if (animationsRes.isNotEmpty()) {
            val animSet = ArrayList<String>()
            animationsRes.mapTo(animSet) { weakActivity.getLocalizedRaw(it, locale) }

            val animation = services.actuation.async()?.makeAnimation(animSet).await()
            val animate = services.actuation.async()?.makeAnimate(robotContext, animation).await()

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
            services.conversation.async()?.makePhraseSet(phrases).await()
        }

        val listen = if (locale == null) {
            services.conversation.async()?.makeListen(robotContext, phraseSet, bodyLanguageOption, getCurrentLocale().toNaoqiLocale()).await()
        } else {
            services.conversation.async()?.makeListen(robotContext, phraseSet, bodyLanguageOption, locale.toNaoqiLocale()).await()
        }

        onStart?.let {
            listen.async().setOnStartedListener { ui { it() } }//FIXME QiSDK 1.1.15 .await()
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

        val animation = services.actuation.async()?.makeAnimation(animSet).await()
        val animate = services.actuation.async()?.makeAnimate(robotContext, animation).await()

        onStart?.let {
            animate.async().setOnStartedListener { ui { it() } }//FIXME QiSDK 1.1.15 .await()
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

        val topicSet = ArrayList<Topic>()
        topicSet.add(services.conversation.async()?.makeTopic(weakActivity.getLocalizedRaw(mainTopic, locale)).await())
        additionalTopics.mapTo(topicSet) { services.conversation.async()?.makeTopic(weakActivity.getLocalizedRaw(it, locale)).await() }

        val discuss = if (locale == null) {
            services.conversation.async()?.makeDiscuss(robotContext, topicSet, getCurrentLocale().toNaoqiLocale()).await()
        } else {
            services.conversation.async()?.makeDiscuss(robotContext, topicSet, locale.toNaoqiLocale()).await()
        }

        val future = discuss.async().run()
        discuss.async().setOnStartedListener {
            ui {
                onStart?.invoke()
                if (gotoBookmark != null) {
                    info("Go to bookmark : $gotoBookmark")
                    val bookmark = topicSet[0].async().bookmarks.await()[gotoBookmark]
                    discuss.async().goToBookmarkedOutputUtterance(bookmark).await()
                }
            }
        }//FIXME QiSDK 1.1.15 .await()
        return handleFuture(future, onResult, throwOnStop, Action.TALKING, Action.LISTENING)
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
    ): String? {

        val topics = discussion.topics.map { (key, value) ->
            val top = services.conversation.async()?.makeTopic(value).await()
            key to top
        }.toMap()

        val discuss = if (discussion.locale == null) {
            services.conversation.async()?.makeDiscuss(robotContext, topics.values.toList(), getCurrentLocale().toNaoqiLocale()).await()
        } else {
            services.conversation.async()?.makeDiscuss(robotContext, topics.values.toList(), discussion.locale.toNaoqiLocale()).await()
        }

        var startBookmark: String? = null
        discussion.prepare(discuss, topics)?.let {
            startBookmark = it
        }
        gotoBookmark?.let {
            startBookmark = gotoBookmark
        }
        discuss.async().setOnStartedListener {
            ui {
                onStart?.invoke()
                if (startBookmark != null) {
                    info("Go to bookmark : $startBookmark")
                    val bookmark = topics[discussion.mainTopic]?.async()?.bookmarks.await()[startBookmark]
                    discuss.async().goToBookmarkedOutputUtterance(bookmark).await()
                }
            }
        }//FIXME QiSDK 1.1.15 .await()
        val future = discuss.async().run()
        return handleFuture(future, onResult, throwOnStop, Action.TALKING, Action.LISTENING)
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



}