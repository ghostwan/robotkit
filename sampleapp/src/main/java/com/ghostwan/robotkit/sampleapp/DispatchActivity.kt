package com.ghostwan.robotkit.sampleapp

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import com.ghostwan.robotkit.naoqi.robot.isOnLocalPepper
import kotlinx.android.synthetic.main.activity_dispatch.*


class DispatchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dispatch)

        val info = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)

        val list = info.activities
                .filter {!it.name.contains(javaClass.simpleName)}
                .map { DecoratedActivityInfo(it) }
        val spinner = findViewById<View>(R.id.spinner) as Spinner
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, list)

        launchButton.setOnClickListener {

            if(isOnLocalPepper()) {
                val activityInfo = (spinner.selectedItem as DecoratedActivityInfo).activityInfo
                val intent = Intent()
                intent.setClassName(activityInfo.applicationInfo.packageName, activityInfo.name)
                startActivity(intent)
            }
            else {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(getString(R.string.ip_address))

                val input = EditText(this)
                input.inputType = InputType.TYPE_CLASS_TEXT
                builder.setView(input)

                builder.setNegativeButton(R.string.cancel) { dialog, which -> dialog.cancel() }
                builder.setPositiveButton(R.string.ok)  { dialog, which ->
                    val address = "tcp://${input.text}:9559"
                    val activityInfo = (spinner.selectedItem as DecoratedActivityInfo).activityInfo
                    val intent = Intent()
                    intent.setClassName(activityInfo.applicationInfo.packageName, activityInfo.name)
                    intent.putExtra("address", address)
                    startActivity(intent)
                }

                builder.show()
            }

        }
    }


    class DecoratedActivityInfo(val activityInfo: ActivityInfo) {
        override fun toString() : String = this.activityInfo.name
    }

}
