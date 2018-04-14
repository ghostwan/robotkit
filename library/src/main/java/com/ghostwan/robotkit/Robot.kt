package com.ghostwan.robotkit

import com.ghostwan.robotkit.`object`.Action

interface Robot {

    /**
     * Connect the robot
     */
    suspend fun connect()

    /**
     * Disconnect the robot
     */
    suspend fun disconnect()

    /**
     * Set the callback called when the connection with robot is lost
     *
     * @param function Lambda called when the connection with robot is lost. Give the reason why.
     */
    fun setOnRobotLost(function: (String) -> Unit)

    /**
     * Tell if the connection with robot is still working
     *
     * @return if the robot is connected
     */
    fun isConnected() : Boolean

    /**
     * Stop all or a specific set of actions
     *
     * If no action is specify stop all NaoqiRobot's actions running
     *
     * @param actions list of actions to stop
     */
    fun stop(vararg actions : Action)

}