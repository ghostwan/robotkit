package com.ghostwan.robotkit.naoqi.ext

import java.util.*

fun ClosedRange<Int>.random() = Random().nextInt(endInclusive - start) +  start