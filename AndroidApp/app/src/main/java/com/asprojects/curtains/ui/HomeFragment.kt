package com.asprojects.curtains.ui

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import com.asprojects.curtains.*
import com.google.android.material.textfield.TextInputEditText

class HomeFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        val enable_switch : ToggleButton = root.findViewById(R.id.enable_switch)
        enable_switch.isChecked = getIsEnabled()
        enable_switch.setOnCheckedChangeListener { _, isChecked ->  setIsEnabled(isChecked) }

        val override_enable_switch : Switch = root.findViewById(R.id.override_enable)
        override_enable_switch.isChecked = getNextDayOverrideEnabled()
        override_enable_switch.setOnCheckedChangeListener { _, isChecked ->  setNextDayOverrideEnabled(isChecked) }

        val override_time_field : TextInputEditText = root.findViewById(R.id.override_time)
        override_time_field.setText(getTimeStringFromInt(getNextDayOverrideTime()))
        override_time_field.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                setNextDayOverrideTime(getIntFromTimeString(override_time_field.text.toString()))
                override_time_field.setText(getTimeStringFromInt(getNextDayOverrideTime()))
            }
        }
        override_time_field.setOnKeyListener{ _, keyCode, event ->
            if ((event.action == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                setNextDayOverrideTime(getIntFromTimeString(override_time_field.text.toString()))
                override_time_field.setText(getTimeStringFromInt(getNextDayOverrideTime()))
                true
            }
            else false
        }

        val override_length_field : TextInputEditText = root.findViewById(R.id.override_length)
        override_length_field.setText(getTimeStringFromInt(getNextDayOverrideLength()))
        override_length_field.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                setNextDayOverrideLength(getIntFromTimeString(override_length_field.text.toString()))
                override_length_field.setText(getTimeStringFromInt(getNextDayOverrideLength()))
            }
        }
        override_length_field.setOnKeyListener{ _, keyCode, event ->
            if ((event.action == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                setNextDayOverrideLength(getIntFromTimeString(override_length_field.text.toString()))
                override_length_field.setText(getTimeStringFromInt(getNextDayOverrideLength()))
                true
            }
            else false
        }

        return root
    }
}