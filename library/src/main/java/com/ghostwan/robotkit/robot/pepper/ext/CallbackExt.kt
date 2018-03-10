package com.ghostwan.robotkit.robot.pepper.ext

import android.content.Context
import com.aldebaran.qi.AnyObject
import com.aldebaran.qi.Session
import com.aldebaran.qi.sdk.core.FocusManager
import com.aldebaran.qi.sdk.core.SessionManager
import com.ghostwan.robotkit.robot.pepper.exception.RobotUnavailableException
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Created by erwan on 10/03/2018.
 */
suspend fun SessionManager.await(context: Context, robotCallback: (String) -> Unit): Session =
        suspendCoroutine { cont ->
            val callback = object : SessionManager.Callback {
                override fun onRobotAbsent() {
                    cont.resumeWithException(RobotUnavailableException("robot absent"))
                }

                override fun onRobotReady(session: Session?) {
                    cont.resume(session!!)
                }

                override fun onRobotLost() {
                    robotCallback("Robot lost")
                }
            }
            register(context, callback)
        }






suspend fun FocusManager.await(robotCallback: (String) -> Unit): AnyObject =
        suspendCoroutine { cont ->
            val callback = object : FocusManager.Callback {
                override fun onFocusLost() {
                    robotCallback("Focus lost")
                }

                override fun onFocusGained(robotContext: AnyObject?) {
                    cont.resume(robotContext!!)
                }

                override fun onFocusRefused(reason: String?) {
                    cont.resumeWithException(RobotUnavailableException("focus refused : $reason"))
                }
            }
            register(callback)
        }

