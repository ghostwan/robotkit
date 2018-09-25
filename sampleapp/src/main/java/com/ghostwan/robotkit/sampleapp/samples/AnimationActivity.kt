package com.ghostwan.robotkit.sampleapp.samples

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.ghostwan.robotkit.naoqi.`object`.*
import com.ghostwan.robotkit.naoqi.robot.LocalPepper
import com.ghostwan.robotkit.util.ui

class AnimationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui {
            val pepper = LocalPepper(this@AnimationActivity)
            pepper.connect()

            pepper.say("Hello I'going to play an animation")

            val anim = animation {
                actuator(Actuator.HeadPitch) {
                    key(31, -0.9019809)
                    key(46, -0.59166664) {
                        leftTangent(-5.0, 0.0)
                        rightTangent(-5.0, 0.0)
                    }
                }
            }

            pepper.animate(anim)

        }

    }

}