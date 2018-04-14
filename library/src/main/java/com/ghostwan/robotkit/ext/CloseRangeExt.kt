package com.ghostwan.robotkit.ext

import java.util.*

fun ClosedRange<Int>.random() = Random().nextInt(endInclusive - start) +  start