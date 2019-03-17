package com.ghostwan.robotkit.ext

import android.media.MediaPlayer
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


/**
 * Created by erwan on 10/03/2018.
 */
suspend fun MediaPlayer.startAndAwait() =
        suspendCoroutine { cont: Continuation<Unit> ->
            this.setOnCompletionListener { cont.resume(Unit) }
            this.setOnErrorListener { _, what, _ -> cont.resumeWithException(RuntimeException("Media player error : $what"))
                return@setOnErrorListener true
            }
            this.start()
        }

fun MediaPlayer.stopAndRelease() {
    stop()
    release()
}