package com.ghostwan.robotkit.`object`

/**
 * Result of a computation
 *
 * Can be used like this:
 *
 * when(result) {
 *      is Success -> displayInfo("action succeed")
 *      is Failure -> displayError(result.exception)
 *  }
 */
sealed class Result<T>

/**
 * If the result is a success
 * @param value The value of the successful computation
 */
data class Success<T>(val value: T) : Result<T>()

/**
 * If the result is a failure
 *
 * @param exception The exception thrown if the computation failed
 */
data class Failure<T>(val exception: Throwable) : Result<T>()
