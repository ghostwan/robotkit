package com.ghostwan.robotkit.naoqi.pepper

import android.app.Activity
import com.aldebaran.qi.sdk.`object`.actuation.Actuation
import com.aldebaran.qi.sdk.`object`.conversation.*
import com.aldebaran.qi.sdk.core.FocusManager
import com.aldebaran.qi.sdk.core.SessionManager
import com.ghostwan.robotkit.naoqi.ext.*
import com.ghostwan.robotkit.util.*


/**
 * This class is useful if the app is running on NaoqiRobot's tablet
 *
 * If the app is running on pepper's tablet this class will handle the connection to RobotService
 * to retrieve the endpoints and authenticated tokens.
 *
 */
class LocalPepper(activity: Activity) : Pepper(activity) {

    private var focusManager: FocusManager? = null

    override suspend fun connect(address: String?) {

        if (isConnected()) {
            disconnect()
        }

        val sessionManager = SessionManager(false)
        focusManager = FocusManager(weakActivity, sessionManager)

        val robotContextAO = focusManager!!.await(robotLostListener)
        robotContext = util.deserializeRobotContext(robotContextAO)
        info("session connected")
        session = sessionManager.await(weakActivity, {
            focusManager?.unregister()
            robotLostListener?.invoke(it)
        })
        info("focus retrieved")
        conversation = util.retrieveService(session!!, Conversation::class.java, "Conversation")
        actuation = util.retrieveService(session!!, Actuation::class.java, "Actuation")
        info("services retrieved")
    }

    override suspend fun disconnect() {
        if (isConnected()) {
            focusManager?.unregister()
            session?.close()
        }
    }



}