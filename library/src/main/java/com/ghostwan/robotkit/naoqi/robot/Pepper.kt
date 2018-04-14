package com.ghostwan.robotkit.naoqi.robot

import android.app.Activity
import android.provider.Settings
import com.aldebaran.qi.Session
import com.aldebaran.qi.sdk.`object`.focus.FocusOwner
import com.aldebaran.qi.sdk.core.FocusManager
import com.aldebaran.qi.sdk.core.SessionManager
import com.ghostwan.robotkit.naoqi.NaoqiRobot
import com.ghostwan.robotkit.naoqi.ext.await
import com.ghostwan.robotkit.util.info

sealed class Pepper(activity: Activity) : NaoqiRobot(activity) {
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
class LocalPepper(activity: Activity) : Pepper(activity) {

    private var focusManager: FocusManager? = null

    override suspend fun connect() {

        if (isConnected()) {
            disconnect()
        }

        val sessionManager = SessionManager(false)
        focusManager = FocusManager(weakActivity, sessionManager)

        val robotContextAO = focusManager!!.await(robotLostListener)
        robotContext = services.deserializeRobotContext(robotContextAO)
        info("session connected")
        session = sessionManager.await(weakActivity, {
            focusManager?.unregister()
            robotLostListener?.invoke(it)
        })
        info("focus retrieved")
        services.retrieve(session!!)
    }

    override suspend fun disconnect() {
        if (isConnected()) {
            focusManager?.unregister()
            session?.close()
        }
    }

}

class RemotePepper(activity: Activity, private val address: String) : Pepper(activity){

    private lateinit var focusOwner : FocusOwner

    override suspend fun connect() {

        if (isConnected()) {
            disconnect()
        }

        session = Session()
        session?.addConnectionListener(object: Session.ConnectionListener {
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
        session?.connect(address)?.await()

        val deviceId = Settings.Secure.getString(weakActivity.contentResolver, Settings.Secure.ANDROID_ID)
        val packageId = weakActivity.packageName

        services.retrieve(session!!)
        focusOwner = services.focus.async().take().await()
        robotContext = services.contextFactory.async().makeContext().await()
        robotContext!!.async().setFocus(focusOwner).await()
        robotContext!!.async().setIdentity("$deviceId:$packageId").await()

    }

}