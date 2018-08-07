package com.ghostwan.robotkit.naoqi.`object`

import android.util.Log
import com.aldebaran.qi.*
import com.aldebaran.qi.sdk.`object`.AnyObjectProvider
import com.aldebaran.qi.sdk.`object`.conversation.QiChatExecutor
import com.aldebaran.qi.serialization.QiSerializer
import com.ghostwan.robotkit.util.TAG

class AsyncQiChatExecutor(private val serializer: QiSerializer, private val asyncValue: QiChatExecutor.Async) : QiService(), QiChatExecutor, AnyObjectProvider {
    override fun async(): QiChatExecutor.Async {
        return asyncValue
    }

    private val anyObjectProperty by lazy { Property(AnyObject::class.java)  }

     override fun getAnyObject(): AnyObject {
         val builder = DynamicObjectBuilder()
         try {
             builder.advertiseMethods<QiChatExecutor.Async, QiChatExecutor.Async>(serializer, QiChatExecutor.Async::class.java, asyncValue)
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
        return "AsyncQiChatExecutor{ $anyObject }"
    }

}


abstract class SyncQiChatExecutor(private val serializer: QiSerializer) : QiService(), QiChatExecutor, AnyObjectProvider {
    override fun async(): QiChatExecutor.Async {
        return object : QiChatExecutor.Async {
            override fun runWith(params: MutableList<String>?): Future<Void> {
                return Future.of(null).thenConsume { this@SyncQiChatExecutor.runWith(params) }
            }

            override fun stop(): Future<Void> {
                return Future.of(null).thenConsume { this@SyncQiChatExecutor.stop()}
            }
        }
    }

    private val anyObjectProperty by lazy { Property(AnyObject::class.java)  }

    override fun getAnyObject(): AnyObject {
        val builder = DynamicObjectBuilder()
        try {
            builder.advertiseMethods<QiChatExecutor, QiChatExecutor>(serializer, QiChatExecutor::class.java, this)
            builder.setThreadingModel(DynamicObjectBuilder.ObjectThreadingModel.MultiThread)
            builder.advertiseProperty("autonomousReaction", anyObjectProperty)
        } catch (e: Exception) {
            Log.e(TAG, "Advertise error", e)
        }

        return builder.`object`()
    }

    override fun toString(): String {
        return "SyncQiChatExecutor{ $anyObject }"
    }

}