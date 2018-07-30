package com.ghostwan.robotkit.naoqi.robot

import android.app.Activity
import com.ghostwan.robotkit.naoqi.NaoqiRobot

/**
 * Nao robot
 */
open class Nao (activity: Activity, address: String) : NaoqiRobot(activity, address, "nao"){

    override fun getRobotType() : String {
        return "Nao"
    }

}