package com.asprojects.curtains.ui

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.asprojects.curtains.*
import com.google.android.material.textfield.TextInputEditText


class SettingsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater,  container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_settings, container, false)

        val ip_field : TextInputEditText = root.findViewById(R.id.ip_address)
        ip_field.setText(getIP())
        ip_field.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                setIP(ip_field.text.toString())
                ip_field.setText(getIP())
            }
        }

        ip_field.setOnKeyListener{ _, keyCode, event ->
            if ((event.action == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                setIP(ip_field.text.toString())
                ip_field.setText(getIP())
                true
            }
            else false
        }

        val port_field : TextInputEditText = root.findViewById(R.id.ip_port)
        port_field.setText(getIPPort())
        port_field.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                setIPPort(port_field.text.toString())
                port_field.setText(getIPPort())
            }
        }

        port_field.setOnKeyListener{ _, keyCode, event ->
            if ((event.action == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                setIPPort(port_field.text.toString())
                port_field.setText(getIPPort())
                true
            }
            else false
        }

        root.findViewById<Button>(R.id.reconnect).setOnClickListener {
            setIP(ip_field.text.toString(), false)
            setIPPort(port_field.text.toString(), false)
            clearFocus(root)
            reconnect()
        }

        root.findViewById<Button>(R.id.sync_to).setOnClickListener {
            clearFocus(root)
            sync_to()
        }
        root.findViewById<Button>(R.id.sync_from).setOnClickListener {
            clearFocus(root)
            sync_from()
        }

        root.findViewById<Button>(R.id.manual_run).setOnClickListener {
            clearFocus(root)
            manual_run()
        }
        root.findViewById<Button>(R.id.manual_stop).setOnClickListener {
            clearFocus(root)
            manual_stop()
        }

        return root
    }

    fun clearFocus(view: View) {
        view.findViewById<TextInputEditText>(R.id.ip_port).clearFocus()
        view.findViewById<TextInputEditText>(R.id.ip_address).clearFocus()
        (requireActivity() as MainActivity).hideKeyboard(view)
    }
}