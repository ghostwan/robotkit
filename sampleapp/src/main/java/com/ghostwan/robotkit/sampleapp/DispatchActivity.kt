package com.ghostwan.robotkit.sampleapp

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.ghostwan.robotkit.sampleapp.samples.StopActivity
import kotlinx.android.synthetic.main.activity_dispatch.*

class DispatchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dispatch)
        //TODO Add List view to choose activity
        val clazz = StopActivity::class.java

        localButton.setOnClickListener {
            val activityIntent = Intent(this  , clazz)
            startActivity(activityIntent)
            finish()
        }

        remoteButton.setOnClickListener {
            val activityIntent = Intent(this  , clazz)
            activityIntent.putExtra("address", "tcp://10.0.2.2:9559")
            startActivity(activityIntent)
            finish()
        }
    }
}
