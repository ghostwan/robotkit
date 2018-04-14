package com.ghostwan.robotkit

interface Robot {
    /**
     * Connect to the robot using is optional address
     */
    suspend fun connect(address: String? = null)

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
}