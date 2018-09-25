package com.ghostwan.robotkit.naoqi.`object`


data class Animation(val clip: Clip?, val actuatorCurve: List<ActuatorCurve>, val labels: List<Labels>,
                     var fps: Int = 25, val typeVersion: String = "2.0")

data class Clip(val fps: Int = 25, val startFrame: Int, val endFrame:Int)

data class ActuatorCurve(val key: List<Key>, val fps: Int = 25, val actuator: Actuator, val mute: Boolean, val unit: UnitValue)

enum class Actuator { RShoulderRoll, RHand, KneePitch, LElbowYaw, LElbowRoll, LHand, HipPitch, HeadPitch, HeadYaw, HipRoll, RElbowYaw, LShoulderRoll, LShoulderPitch, RShoulderPitch, LWristYaw, RElbowRoll, RWristYaw }

enum class UnitValue { radian, degree, dimensionless, meter }

data class Key(val leftTangent: Tangent?, val rightTangent: Tangent?, val frame: Int, val value: Double, val smooth: Boolean = false, val symmetrical: Boolean = false)

data class Tangent(val abscissaParam:Float, val interpType: InterpType, val ordinateParam: Float, val side:Side)

enum class InterpType{bezier_auto, bezier, linear}
enum class Side{left, right}

data class Labels(val label:List<Label>, val fps:Int = 25, val groupName:String)

data class Label(val name: String, val frame: Int)


fun animation(setup: () -> Unit) : Animation {

}

fun actuator(actuator: Actuator, setup: () -> Unit) : Animation {

}

fun key(frame: Int, value: Double, setup: (() -> Unit)? = null) : Animation {

}

fun leftTangent(abscissaParam: Float, ordinateParam: Float) : Animation {

}

fun rightTangent(abscissaParam: Float, ordinateParam: Float) : Animation {

}

