package com.ghostwan.robotkit.naoqi.robot

import android.app.Activity
import com.aldebaran.qi.sdk.core.FocusManager
import com.aldebaran.qi.sdk.core.SessionManager
import com.ghostwan.robotkit.naoqi.NaoqiRobot
import com.ghostwan.robotkit.naoqi.ext.await
import com.ghostwan.robotkit.util.info

/**
 * Pepper robot
 */
open class Pepper(activity: Activity, address: String?) : NaoqiRobot(activity, address) {
    override fun getRobotType() : String {
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
class LocalPepper(activity: Activity) : Pepper(activity, null) {

    private var focusManager: FocusManager? = null

    override suspend fun connect() {

        if (isConnected()) {
            disconnect()
        }

        val sessionManager = SessionManager(false)
        focusManager = FocusManager(weakActivity, sessionManager)

        val robotContextAO = focusManager!!.await(robotLostListener)
        info("session connected")
        val session = sessionManager.await(weakActivity, {
            focusManager?.unregister()
            robotLostListener?.invoke(it)
        })
        info("focus retrieved")
        initNaoqiData(session, robotContextAO)
    }

    override suspend fun disconnect() {
        if (isConnected()) {
            focusManager?.unregister()
            super.disconnect()
        }
    }

}
