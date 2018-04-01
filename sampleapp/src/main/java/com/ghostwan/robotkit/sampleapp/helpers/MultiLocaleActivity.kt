package com.ghostwan.robotkit.sampleapp.helpers

import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import com.ghostwan.robotkit.sampleapp.R
import kotlinx.android.synthetic.main.activity_multi_locales.*
import java.util.*


abstract class MultiLocaleActivity : ParentActivity() {

    lateinit var locale : Locale

    override fun defaultLayout(): Int = R.layout.activity_multi_locales

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locale = Locale.getDefault()
        when(locale.language) {
            Locale.FRENCH.language -> radio_french.isChecked = true
            Locale.ENGLISH.language -> radio_english.isChecked = true
        }
        radio_french.setOnClickListener { onRadioButtonClicked(it) }
        radio_english.setOnClickListener { onRadioButtonClicked(it) }
    }

    fun onRadioButtonClicked(view: View) {
        // Is the button now checked?
        val checked = (view as RadioButton).isChecked

        // Check which radio button was clicked
        when (view.getId()) {
            R.id.radio_english -> if (checked)  {
                locale = Locale.ENGLISH
            }
            R.id.radio_french -> if (checked) {
                locale = Locale.FRENCH
            }
        }
    }

}
