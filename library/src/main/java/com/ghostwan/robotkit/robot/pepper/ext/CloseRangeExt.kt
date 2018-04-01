package com.ghostwan.robotkit.robot.pepper.ext

import java.util.*

fun ClosedRange<Int>.random() = Random().nextInt(endInclusive - start) +  start