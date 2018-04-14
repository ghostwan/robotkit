package com.ghostwan.robotkit.naoqi.ext

import com.aldebaran.qi.Future
import kotlinx.coroutines.experimental.CancellationException
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Created by erwan on 10/03/2018.
 */
suspend fun <T> Future<T>?.await(): T =
        suspendCoroutine { cont: Continuation<T> ->
            this?.thenConsume {
                when {
                    it.isSuccess -> cont.resume(it.value)
                    it.isCancelled -> cont.resumeWithException(CancellationException())
                    else -> cont.resumeWithException(it.error)
                }
            }
        }



