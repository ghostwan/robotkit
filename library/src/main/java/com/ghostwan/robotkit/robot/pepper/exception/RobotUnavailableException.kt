package com.ghostwan.robotkit.robot.pepper.exception

/**
 * Thrown if the robot is unavailable
 *
 * @param message The reason why the robot is unavailable
 */
class RobotUnavailableException(message: String?) : Exception(message)