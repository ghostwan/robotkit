package com.ghostwan.robotkit.naoqi.`object`

import android.util.Log
import com.aldebaran.qi.*
import com.aldebaran.qi.sdk.`object`.AnyObjectProvider
import com.aldebaran.qi.sdk.`object`.conversation.QiChatExecutor
import com.aldebaran.qi.sdk.util.FutureUtils
import com.aldebaran.qi.serialization.QiSerializer
import com.ghostwan.robotkit.util.TAG

abstract class RKQiChatExecutor(val serializer: QiSerializer) : QiService(), QiChatExecutor, AnyObjectProvider {

    private val anyObjectProperty by lazy { Property(AnyObject::class.java)  }

     override fun getAnyObject(): AnyObject {
         val builder = DynamicObjectBuilder()
         try {
             builder.advertiseMethods<QiChatExecutor, RKQiChatExecutor>(serializer, QiChatExecutor::class.java, this)
             builder.advertiseProperty("autonomousReaction", anyObjectProperty)
         } catch (e: Exception) {
             Log.e(TAG, "Advertise error", e)
         }

         return builder.`object`()
     }

    override fun async(): QiChatExecutor.Async {
        return object : QiChatExecutor.Async {

            override fun runWith(params: List<String>): Future<Void> {
                return FutureUtils.futureOf { this@RKQiChatExecutor.runWith(params) }
            }

            override fun stop(): Future<Void> {
                return FutureUtils.futureOf { this@RKQiChatExecutor.stop() }
            }

        }
    }

    abstract override fun runWith(params: List<String>)

    abstract override fun stop()

    override fun toString(): String {
        return "RKQiChatExecutor{ $anyObject }"
    }
}