package com.ghostwan.robotkit.robot.pepper

/**
 * Created by epinault on 13/03/2018.
 */
class Result<T> {

    var value : T? = null
    var exception : Throwable? = null

    constructor(value : T) {
        this.value = value
    }

    constructor(exception : Throwable) {
        this.exception = exception
    }
}