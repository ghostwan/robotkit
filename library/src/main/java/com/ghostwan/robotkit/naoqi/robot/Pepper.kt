package com.ghostwan.robotkit.naoqi.robot

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import com.aldebaran.qi.Session
import com.aldebaran.qi.UserTokenAuthenticator
import com.aldebaran.qi.sdk.core.util.FocusUtil
import com.aldebaran.robotservice.IRobotService
import com.ghostwan.robotkit.exception.RobotUnavailableException
import com.ghostwan.robotkit.ext.getLocalService
import com.ghostwan.robotkit.naoqi.NaoqiRobot
import com.ghostwan.robotkit.naoqi.NaoqiServices
import com.ghostwan.robotkit.naoqi.ext.await
import com.ghostwan.robotkit.util.errorLog
import com.ghostwan.robotkit.util.infoLog
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

private const val ROBOT_SERVICE_PACKAGE = "com.aldebaran.robotservice"

/**
 * Pepper robot
 */
open class Pepper(activity: Activity, address: String?, password: String?) : NaoqiRobot(activity, address, password) {
    override fun getRobotType(): String {
        return "Pepper"
    }
}

/**
 * This class is useful if the app is running on Pepper's tablet
 *
 * If the app is running on pepper's tablet this class will handle the connection to RobotService
 * to retrieve the endpoints and authenticated tokens.
 *
 */
class LocalPepper(activity: Activity) : Pepper(activity, null, null) {

    private var robotService: IRobotService? = null

    companion object {
        private const val ACTION_ROBOT_SERVICE = "com.aldebaran.action.ROBOT_SERVICE"
        private const val USER_SESSION = "tablet"
        private val ACTION_FOCUS_PREEMPTED = "com.aldebaran.action.FOCUS_PREEMPTED"
        private val ACTION_FOCUS_REFUSED = "com.aldebaran.action.FOCUS_REFUSED"
        private val EXTRA_FOCUS_REFUSED_REASON = "com.aldebaran.extra.EXTRA_FOCUS_REFUSED_REASON"
    }

    override suspend fun connect() {

        if (isConnected()) {
            disconnect()
        }
        session = Session()
        services = NaoqiServices(session)

        val localRobotService = weakActivity.getLocalService(ROBOT_SERVICE_PACKAGE, ACTION_ROBOT_SERVICE)

        if(localRobotService.binder == null)
                throw RobotUnavailableException("RobotService not available!")

        robotService = IRobotService.Stub.asInterface(localRobotService.binder)
        val endpoint = robotService?.publicEndpoint
        val token = robotService?.publicToken

        session.setClientAuthenticator(UserTokenAuthenticator(USER_SESSION, token))
        session.addConnectionListener(object: Session.ConnectionListener {
            override fun onConnected() {
            }

            override fun onDisconnected(reason: String?) {
                if(reason == null) {
                    robotLostListener?.invoke("")
                }else {
                    robotLostListener?.invoke(reason)
                }
            }

        })
        session.connect(endpoint)?.await()
        infoLog("session connected")

        takeFocus()

        weakActivity.unbindService(localRobotService.connection)
    }

    override suspend fun hasFocus(): Boolean {
        if(robotService == null)
            return false
        val activityName = weakActivity.componentName.className
        val activityId = FocusUtil.extractActivityId(weakActivity)
        return robotService?.isFocusPreemptedWithID(activityName, activityId) ?: false
    }

    override suspend fun takeFocus() {
        takeFocus(getToken())
    }

    private suspend fun getToken(): String? {
        return suspendCoroutine { cont: Continuation<String> ->
            if(robotService == null) {
                cont.resumeWithException(RobotUnavailableException("RobotService not available!"))
            } else {
                val focusToken = FocusUtil.extractFocusToken(weakActivity)
                val activityName = weakActivity.componentName.className
                val activityId = FocusUtil.extractActivityId(weakActivity)

                val focusBroadcastReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        when (intent?.action) {
                            ACTION_FOCUS_PREEMPTED -> {
                                if (FocusUtil.isFocusPreempted(focusToken, activityName, intent)) {
                                    weakActivity.unregisterReceiver(this)
                                    infoLog("focus token retrieved")
                                    cont.resume(focusToken)
                                }
                            }
                            ACTION_FOCUS_REFUSED -> {
                                weakActivity.unregisterReceiver(this)
                                errorLog("focus refused!")
                                cont.resumeWithException(RobotUnavailableException(intent.getStringExtra(EXTRA_FOCUS_REFUSED_REASON)))
                            }
                        }
                    }
                }
                weakActivity.registerReceiver(focusBroadcastReceiver, IntentFilter(ACTION_FOCUS_PREEMPTED))
                weakActivity.registerReceiver(focusBroadcastReceiver, IntentFilter(ACTION_FOCUS_REFUSED))

                if (activityId != null && !activityId.isEmpty() && robotService!!.isFocusPreemptedWithID(activityName, activityId)) {
                    weakActivity.unregisterReceiver(focusBroadcastReceiver)
                    infoLog("focus token retrieved")
                    cont.resume(focusToken)
                }
            }
        }
    }

}

fun Context.isOnLocalPepper(): Boolean {
    return try {
        packageManager.getApplicationInfo(ROBOT_SERVICE_PACKAGE, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

}
