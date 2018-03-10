package com.ghostwan.robotkit.robot.pepper

import android.app.Activity
import android.content.Context
import android.util.Log
import com.aldebaran.qi.AnyObject
import com.aldebaran.qi.Future
import com.aldebaran.qi.QiConversionException
import com.aldebaran.qi.Session
import com.aldebaran.qi.sdk.`object`.actuation.ActuationConverter
import com.aldebaran.qi.sdk.`object`.autonomousabilities.AutonomousabilitiesConverter
import com.aldebaran.qi.sdk.`object`.context.ContextConverter
import com.aldebaran.qi.sdk.`object`.context.RobotContext
import com.aldebaran.qi.sdk.`object`.conversation.Conversation
import com.aldebaran.qi.sdk.`object`.conversation.ConversationConverter
import com.aldebaran.qi.sdk.`object`.conversation.Phrase
import com.aldebaran.qi.sdk.`object`.focus.FocusConverter
import com.aldebaran.qi.sdk.`object`.human.HumanConverter
import com.aldebaran.qi.sdk.`object`.humanawareness.HumanawarenessConverter
import com.aldebaran.qi.sdk.`object`.knowledge.KnowledgeConverter
import com.aldebaran.qi.sdk.`object`.sharedtopics.SharedtopicsConverter
import com.aldebaran.qi.sdk.`object`.touch.TouchConverter
import com.aldebaran.qi.sdk.core.FocusManager
import com.aldebaran.qi.sdk.core.SessionManager
import com.aldebaran.qi.sdk.serialization.EnumConverter
import com.aldebaran.qi.serialization.QiSerializer
import com.ghostwan.robotkit.robot.pepper.exception.RobotUnavailableException
import com.ghostwan.robotkit.robot.pepper.ext.await
import java.util.concurrent.ExecutionException


/**
 * Created by erwan on 10/03/2018.
 */
class MyPepper(activity: Activity) {

    private val serializer: QiSerializer = createQiSerializer()
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
        robotContext = deserializeRobotContext(focusManager.await(callback))
        Log.i(TAG, "focus retrieved")
        session = sessionManager.await(context, callback)
        Log.i(TAG, "session connected")
        conversation = retrieveService(session!!, Conversation::class.java, "Conversation")
        Log.i(TAG, "conversation retrieved")
    }


    private suspend fun <T> retrieveService(session: Session, clazz: Class<T>, name: String): T {
        try {
            val service = session.service(name).await()
            val deserialize = serializer.deserialize(service, clazz)
            return clazz.cast(deserialize)
        } catch (e: ExecutionException) {
            throw RobotUnavailableException("Service $name is not available")
        }
    }

    private fun createQiSerializer(): QiSerializer {
        val serializer = QiSerializer()

        serializer.addConverter(EnumConverter())
        serializer.addConverter(ActuationConverter())
        serializer.addConverter(AutonomousabilitiesConverter())
        serializer.addConverter(ContextConverter())
        serializer.addConverter(FocusConverter())
        serializer.addConverter(ConversationConverter())
        serializer.addConverter(HumanConverter())
        serializer.addConverter(TouchConverter())
        serializer.addConverter(KnowledgeConverter())
        serializer.addConverter(SharedtopicsConverter())
        serializer.addConverter(HumanawarenessConverter())

        return serializer
    }

    private fun deserializeRobotContext(robotContext: AnyObject): RobotContext {
        return try {
            val deserialize = serializer.deserialize(robotContext, RobotContext::class.java)
            RobotContext::class.java.cast(deserialize)
        } catch (e: QiConversionException) {
            throw RobotUnavailableException("Error when deserialize robot context")
        }

    }

    private var futureSay: Future<Void>? = null

    suspend fun say(res: Int) {
        val say = conversation?.async()?.makeSay(robotContext, Phrase(context.getString(res))).await()
        futureSay = say.async().run()
        futureSay.await()
    }

}