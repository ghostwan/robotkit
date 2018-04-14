package com.ghostwan.robotkit.naoqi.robot

import android.app.Activity
import android.provider.Settings
import com.aldebaran.qi.Session
import com.aldebaran.qi.sdk.`object`.focus.FocusOwner
import com.ghostwan.robotkit.naoqi.NaoqiRobot
import com.ghostwan.robotkit.naoqi.ext.await

class Nao (activity: Activity, private val address: String) : NaoqiRobot(activity){

    private lateinit var focusOwner : FocusOwner

    override fun getRobotType() : String {
        return "Nao"
    }

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