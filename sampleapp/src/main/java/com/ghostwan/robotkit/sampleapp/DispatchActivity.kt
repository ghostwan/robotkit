package com.ghostwan.robotkit.sampleapp

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.ghostwan.robotkit.sampleapp.scenario.ListenActivity

class DispatchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dispatch)
        //TODO Add List view to choose activity
        startActivity(Intent(this  , ListenActivity::class.java))
        finish()
    }
}
