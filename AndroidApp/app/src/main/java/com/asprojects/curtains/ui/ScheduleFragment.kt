package com.asprojects.curtains.ui

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import androidx.fragment.app.Fragment
import com.asprojects.curtains.*
import com.google.android.material.textfield.TextInputEditText

class ScheduleFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_schedule, container, false)
        val layout: LinearLayout = root.findViewById(R.id.schedule_layout)

        for (i in 0 until layout.childCount) {
            var row: LinearLayout = layout.getChildAt(i) as LinearLayout

            var time_field: TextInputEditText = row.findViewWithTag("time")
            var length_field: TextInputEditText = row.findViewWithTag("length")
            var enable_switch: Switch = row.findViewWithTag("enable")

            enable_switch.isChecked = getDayEnabled(i)
            enable_switch.setOnCheckedChangeListener { _, isChecked ->  setDayEnabled(i, isChecked) }

            time_field.setText(getTimeStringFromInt(getDayTime(i)))
            time_field.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    setDayTime(i, getIntFromTimeString(time_field.text.toString()))
                    time_field.setText(getTimeStringFromInt(getDayTime(i)))
                }
            }
            time_field.setOnKeyListener{ _, keyCode, event ->
                if ((event.action == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    setDayTime(i, getIntFromTimeString(time_field.text.toString()))
                    time_field.setText(getTimeStringFromInt(getDayTime(i)))
                    true
                }
                else false
            }

            length_field.setText(getTimeStringFromInt(getDayLength(i)))
            length_field.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    setDayLength(i, getIntFromTimeString(length_field.text.toString()))
                    length_field.setText(getTimeStringFromInt(getDayLength(i)))
                }
            }
            length_field.setOnKeyListener{ _, keyCode, event ->
                if ((event.action == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    setDayLength(i, getIntFromTimeString(length_field.text.toString()))
                    length_field.setText(getTimeStringFromInt(getDayLength(i)))
                    true
                }
                else false
            }

        }

        return root
    }
}