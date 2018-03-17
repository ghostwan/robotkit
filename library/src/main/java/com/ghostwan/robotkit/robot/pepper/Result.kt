package com.ghostwan.robotkit.robot.pepper

/**
 * Created by epinault on 13/03/2018.
 */
sealed class Result<T>
data class Success<T>(val value: T) : Result<T>()
data class Failure<T>(val exception: Throwable) : Result<T>()
