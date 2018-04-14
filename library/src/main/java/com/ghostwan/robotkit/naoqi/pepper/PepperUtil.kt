package com.ghostwan.robotkit.naoqi.pepper

import com.aldebaran.qi.AnyObject
import com.aldebaran.qi.QiConversionException
import com.aldebaran.qi.Session
import com.aldebaran.qi.sdk.`object`.actuation.ActuationConverter
import com.aldebaran.qi.sdk.`object`.autonomousabilities.AutonomousabilitiesConverter
import com.aldebaran.qi.sdk.`object`.context.ContextConverter
import com.aldebaran.qi.sdk.`object`.context.RobotContext
import com.aldebaran.qi.sdk.`object`.conversation.ConversationConverter
import com.aldebaran.qi.sdk.`object`.focus.FocusConverter
import com.aldebaran.qi.sdk.`object`.human.HumanConverter
import com.aldebaran.qi.sdk.`object`.humanawareness.HumanawarenessConverter
import com.aldebaran.qi.sdk.`object`.knowledge.KnowledgeConverter
import com.aldebaran.qi.sdk.`object`.sharedtopics.SharedtopicsConverter
import com.aldebaran.qi.sdk.`object`.touch.TouchConverter
import com.aldebaran.qi.sdk.serialization.EnumConverter
import com.aldebaran.qi.serialization.QiSerializer
import com.ghostwan.robotkit.exception.RobotUnavailableException
import com.ghostwan.robotkit.naoqi.ext.await
import com.ghostwan.robotkit.util.info
import java.util.concurrent.ExecutionException

/**
 * Utility class to handle pepper robot
 */
class PepperUtil {

    private val serializer: QiSerializer = createQiSerializer()

    /**
     * Simplify Naoqi's Service retrieval
     *
     * @param session Naoqi's session
     * @param clazz Class of the service to use for deserialization
     * @param name Name of the service in Naoqi service directory
     *
     * @return a Naoqi service wrapped in a java object
     */
    suspend fun <T> retrieveService(session: Session, clazz: Class<T>, name: String): T {
        try {
            val service = session.service(name).await()
            val deserialize = serializer.deserialize(service, clazz)
            val data =  clazz.cast(deserialize)
            info("$name retrieved")
            return data
        } catch (e: ExecutionException) {
            throw RobotUnavailableException("Service $name is not available")
        }
    }

    /**
     * Initialize Naoqi AnyObject converter
     *
     * @return a serializer that can be used to deserialize Naoqi's object.
     */
    fun createQiSerializer(): QiSerializer {
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

    /**
     * Helper to deserialize Naoqi robot context.
     *
     * @param robotContext AnyObject that represent Naoqi robot context.
     *
     * @return the robot context wrapped in a java object.
     */
    fun deserializeRobotContext(robotContext: AnyObject): RobotContext {
        return try {
            val deserialize = serializer.deserialize(robotContext, RobotContext::class.java)
            RobotContext::class.java.cast(deserialize)
        } catch (e: QiConversionException) {
            throw RobotUnavailableException("Error when deserialize robot context")
        }

    }
}