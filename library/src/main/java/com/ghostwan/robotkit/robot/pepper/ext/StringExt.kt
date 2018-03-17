package com.ghostwan.robotkit.robot.pepper.ext

import java.security.MessageDigest

/**
 * Created by erwan on 15/03/2018.
 */
fun String.sha512(): String {
    return this.hashWithAlgorithm("SHA-256")
}

private fun String.hashWithAlgorithm(algorithm: String): String {
    val digest = MessageDigest.getInstance(algorithm)
    val bytes = digest.digest(this.toByteArray(Charsets.UTF_8))
    return bytes.fold("", { str, it -> str + "%02x".format(it) })
}