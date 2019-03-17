package com.ghostwan.robotkit.naoqi

import com.aldebaran.qi.AnyObject
import com.aldebaran.qi.QiConversionException
import com.aldebaran.qi.Session
import com.aldebaran.qi.sdk.`object`.AnyObjectWrapperConverter
import com.aldebaran.qi.sdk.`object`.actuation.Actuation
import com.aldebaran.qi.sdk.`object`.actuation.ActuationConverter
import com.aldebaran.qi.sdk.`object`.autonomousabilities.AutonomousabilitiesConverter
import com.aldebaran.qi.sdk.`object`.camera.CameraConverter
import com.aldebaran.qi.sdk.`object`.context.ContextConverter
import com.aldebaran.qi.sdk.`object`.context.RobotContext
import com.aldebaran.qi.sdk.`object`.context.RobotContextFactory
import com.aldebaran.qi.sdk.`object`.conversation.Conversation
import com.aldebaran.qi.sdk.`object`.conversation.ConversationConverter
import com.aldebaran.qi.sdk.`object`.focus.Focus
import com.aldebaran.qi.sdk.`object`.focus.FocusConverter
import com.aldebaran.qi.sdk.`object`.human.HumanConverter
import com.aldebaran.qi.sdk.`object`.humanawareness.HumanawarenessConverter
import com.aldebaran.qi.sdk.`object`.image.ImageConverter
import com.aldebaran.qi.sdk.`object`.knowledge.KnowledgeConverter
import com.aldebaran.qi.sdk.`object`.touch.Touch
import com.aldebaran.qi.sdk.`object`.touch.TouchConverter
import com.aldebaran.qi.sdk.serialization.EnumConverter
import com.aldebaran.qi.serialization.QiSerializer
import com.ghostwan.robotkit.exception.RobotUnavailableException
import com.ghostwan.robotkit.naoqi.`object`.AnyObjectProviderConverter
import com.ghostwan.robotkit.naoqi.ext.await
import com.ghostwan.robotkit.util.infoLog
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.util.concurrent.ExecutionException

class NaoqiServices(session: Session) {

    val serializer: QiSerializer = createQiSerializer()

    val conversation: Deferred<Conversation> by getService(session)

    val touch: Deferred<Touch> by getService(session)

    val actuation: Deferred<Actuation> by getService(session)

    val focus: Deferred<Focus> by getService(session)

    val contextFactory: Deferred<RobotContextFactory> by getService(session, "ContextFactory")

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
            infoLog("$name service retrieved")
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
        val qiSerialiser = QiSerializer()

        qiSerialiser.addConverter(EnumConverter())
        qiSerialiser.addConverter(ActuationConverter())
        qiSerialiser.addConverter(AutonomousabilitiesConverter())
        qiSerialiser.addConverter(ContextConverter())
        qiSerialiser.addConverter(FocusConverter())
        qiSerialiser.addConverter(ConversationConverter())
        qiSerialiser.addConverter(HumanConverter())
        qiSerialiser.addConverter(TouchConverter())
        qiSerialiser.addConverter(KnowledgeConverter())
        qiSerialiser.addConverter(HumanawarenessConverter())
        qiSerialiser.addConverter(CameraConverter())
        qiSerialiser.addConverter(ImageConverter())
        qiSerialiser.addConverter(AnyObjectWrapperConverter())
        qiSerialiser.addConverter(AnyObjectProviderConverter())

        return qiSerialiser
    }

    private inline fun <reified T> getService(session: Session, name: String=T::class.java.simpleName): Lazy<Deferred<T>> {
        return lazy {
            GlobalScope.async {
                retrieveService(session, T::class.java, name)
            }
        }
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