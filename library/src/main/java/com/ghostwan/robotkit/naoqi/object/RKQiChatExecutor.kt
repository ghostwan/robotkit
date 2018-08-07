package com.ghostwan.robotkit.naoqi.`object`

import android.util.Log
import com.aldebaran.qi.AnyObject
import com.aldebaran.qi.DynamicObjectBuilder
import com.aldebaran.qi.Property
import com.aldebaran.qi.QiService
import com.aldebaran.qi.sdk.`object`.AnyObjectProvider
import com.aldebaran.qi.sdk.`object`.conversation.QiChatExecutor
import com.aldebaran.qi.serialization.QiSerializer
import com.ghostwan.robotkit.util.TAG

class RKQiChatExecutor(val serializer: QiSerializer, private val asyncValue: RKQiChatExecutorAsync) : QiService(), QiChatExecutor, AnyObjectProvider {
    override fun async(): QiChatExecutor.Async {
        return asyncValue
    }

    private val anyObjectProperty by lazy { Property(AnyObject::class.java)  }

     override fun getAnyObject(): AnyObject {
         val builder = DynamicObjectBuilder()
         try {
             builder.advertiseMethods<QiChatExecutor.Async, RKQiChatExecutorAsync>(serializer, QiChatExecutor.Async::class.java, asyncValue)
             builder.setThreadingModel(DynamicObjectBuilder.ObjectThreadingModel.MultiThread)
             builder.advertiseProperty("autonomousReaction", anyObjectProperty)
         } catch (e: Exception) {
             Log.e(TAG, "Advertise error", e)
         }

         return builder.`object`()
     }

    override fun runWith(params: List<String>) {asyncValue.runWith(params).get()}

    override fun stop() {asyncValue.stop().get()}

    override fun toString(): String {
        return "RKQiChatExecutor{ $anyObject }"
    }

}

abstract class RKQiChatExecutorAsync : QiChatExecutor.Async