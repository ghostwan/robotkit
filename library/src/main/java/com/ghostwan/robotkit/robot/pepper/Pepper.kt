package com.ghostwan.robotkit.robot.pepper

import android.support.annotation.RawRes
import android.support.annotation.StringRes
import com.aldebaran.qi.sdk.`object`.conversation.BodyLanguageOption
import com.ghostwan.robotkit.robot.pepper.`object`.Action
import com.ghostwan.robotkit.robot.pepper.`object`.Concept
import com.ghostwan.robotkit.robot.pepper.`object`.Discussion
import com.ghostwan.robotkit.robot.pepper.`object`.Result
import java.util.*

/**
 * Interface to call robotics API on Pepper's robots
 */
interface Pepper {

    /**
     * Connect to the robot using pepper's configuration
     */
    suspend fun connect()

    /**
     * Disconnect the robot
     */
    suspend fun disconnect()

    /**
     * @param function Lambda called when the connection with robot is lost. Give the reason why.
     */
    fun setOnRobotLost(function: (String) -> Unit)

    /**
     * @return if pepper is connected
     */
    fun isConnected() : Boolean

    /**
     * If no action is specify stop all Pepper's actions running
     * @param actions list of actions to stop
     */
    fun stop(vararg actions : Action)

    /**
     * Make pepper say a phrase using a string resource and optionally play an animation at the same time
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
                    onStart: (() -> Unit)? = null,
                    onResult: ((Result<Void>) -> Unit)? = null)

    /**
     * Make pepper say a phrase and optionally play an animation at the same time
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
                    onStart: (() -> Unit)? = null,
                    onResult: ((Result<Void>) -> Unit)? = null)
    /**
     * Make pepper listen concepts, where a concept is a group of phrase we want to match
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
                       onStart: (() -> Unit)? = null,
                       onResult: ((Result<Concept>) -> Unit)? = null
    ): Concept?

    /**
     * Animate pepper with an animation using raw qianim file
     *
     * If [onResult] it's not set, the coroutine will be suspended until the animation end, without blocking the thread
     *
     * @param mainAnimation Main animation. It's an android raw resources.
     *
     * @param additionalAnimations Additional animation as for example trajectory to make pepper dance.
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
                        onStart: (() -> Unit)? = null,
                        onResult: ((Result<Void>) -> Unit)? = null)

    /**
     * Make pepper discuss using raw qichat topics
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
                        onStart: (() -> Unit)? = null,
                        onResult: ((Result<String>) -> Unit)? = null
    ): String?

    /**
     * Make pepper discuss using a Discussion object made of qichat topics
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
                        onStart: (() -> Unit)? = null,
                        onResult: ((Result<String>) -> Unit)? = null
    ): String?


}