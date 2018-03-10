package com.ghostwan.robotkit.robot.pepper.ext

import com.aldebaran.qi.Future
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Created by erwan on 10/03/2018.
 */
suspend fun <T> Future<T>?.await(): T =
        suspendCoroutine { cont: Continuation<T> ->
            this?.thenConsume {
                if (it.isSuccess) // the future has been completed normally
                    cont.resume(it.value)
                else // the future has completed with an exception
                    cont.resumeWithException(it.error)
            }
        }



