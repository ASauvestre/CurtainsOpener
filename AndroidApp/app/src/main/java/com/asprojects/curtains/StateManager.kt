package com.asprojects.curtains

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import java.lang.Integer.min
import java.lang.Integer.max

const val MAX_TIME = 24*60-1
const val MIN_LENGTH = 1
const val PREFS_NAME = "CurtainsPrefs"

var activity: MainActivity? = null
var settings: SharedPreferences? = null
var editor: Editor? = null

var is_enabled: Boolean = false

var next_day_override_enabled: Boolean = false
var next_day_override_time: Int = 0
var next_day_override_length: Int = 0

var day_enabled: BooleanArray = BooleanArray(7) { _ -> false }
var day_time: IntArray = IntArray(7) { _ -> 0 }
var day_length: IntArray = IntArray(7) { _ -> 0 }

var connection_status_bool: Boolean = false
var sync_status_bool: Boolean = false

var ip: String = ""
var port: String = ""

fun initState(_activity: MainActivity) {
    activity = _activity
    settings = activity!!.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    editor = settings!!.edit()
}

fun updateConnectionAndSyncStatus() {
    val connectionTextView: TextView = activity!!.findViewById(R.id.connection_status)
    val syncTextView: TextView = activity!!.findViewById(R.id.sync_status)
    when (connection_status_bool) {
        true -> {
            connectionTextView.text = "CONNECTED"
            connectionTextView.setTextColor(
                ResourcesCompat.getColor(
                    activity!!.resources,
                    R.color.happy_green,
                    null
                )
            )
        }
        false -> {
            connectionTextView.text = "DISCONNECTED"
            connectionTextView.setTextColor(
                ResourcesCompat.getColor(
                    activity!!.resources,
                    R.color.sad_red,
                    null
                )
            )
        }
    }

    when (sync_status_bool) {
        true -> {
            syncTextView.text = "SYNCED"
            syncTextView.setTextColor(
                ResourcesCompat.getColor(
                    activity!!.resources,
                    R.color.happy_green,
                    null
                )
            )
        }
        false -> {
            syncTextView.text = "OUT OF SYNC"
            syncTextView.setTextColor(
                ResourcesCompat.getColor(
                    activity!!.resources,
                    R.color.sad_red,
                    null
                )
            )
        }
    }
}

fun getTimeStringFromInt (int: Int) : String {
    val hours = int / 60
    val minutes = int % 60

    if (minutes == -1) return ""

    if (hours > 9) {
        return hours.toString().padStart(2, '0') + ":" + minutes.toString().padStart(2, '0')
    }
    else {
        return hours.toString().padStart(1, '0') + ":" + minutes.toString().padStart(2, '0')
    }
}

fun getIntFromTimeString (string: String) : Int {
    if (string == "") return -1

    var tokens = string.split(":")

    return if (tokens.size == 1) {
        tokens[0].toInt()
    }
    else
    {
        tokens[0].toInt() * 60 + tokens[1].toInt()
    }
}

// Home Screen //

fun setNextDayOverrideEnabled(enabled: Boolean) {
    next_day_override_enabled = enabled
    editor!!.putBoolean("next_day_override_enabled", enabled)
    editor!!.apply()
    sendCommandSetNextDayOverrideEnabled(enabled)
}

fun getNextDayOverrideEnabled(): Boolean {
    next_day_override_enabled = settings!!.getBoolean("next_day_override_enabled", next_day_override_enabled);
    return next_day_override_enabled
}

fun setNextDayOverrideTime(time: Int) {
    next_day_override_time = min(time, MAX_TIME)
    editor!!.putInt("next_day_override_time", next_day_override_time)
    editor!!.apply()
    sendCommandSetNextDayOverrideTime(next_day_override_time)
}

fun getNextDayOverrideTime(): Int {
    next_day_override_time = settings!!.getInt("next_day_override_time", next_day_override_time);
    return next_day_override_time
}

fun setNextDayOverrideLength(length: Int) {
    next_day_override_length = min(length, MAX_TIME)
    next_day_override_length = max(length, MIN_LENGTH)

    editor!!.putInt("next_day_override_length", next_day_override_length)
    editor!!.apply()
    sendCommandSetNextDayOverrideLength(next_day_override_length)
}

fun getNextDayOverrideLength(): Int {
    next_day_override_length = settings!!.getInt("next_day_override_length", next_day_override_length);
    return next_day_override_length
}

fun setIsEnabled(enabled: Boolean) {
    is_enabled = enabled
    editor!!.putBoolean("is_enabled", enabled)
    editor!!.apply()
    sendCommandToggleEnabled(enabled)
}

fun getIsEnabled(): Boolean {
    is_enabled = settings!!.getBoolean("is_enabled", is_enabled);
    return is_enabled
}

// Schedule Screen //

fun setDayEnabled(index: Int, enabled: Boolean) {
    day_enabled[index] = enabled
    editor!!.putBoolean("day_enabled_$index", day_enabled[index] )
    editor!!.apply()
    sendCommandDayEnabled(index, enabled)
}

fun getDayEnabled(index: Int): Boolean {
    day_enabled[index] = settings!!.getBoolean("day_enabled_$index", day_enabled[index]);
    return day_enabled[index]
}

fun setDayTime(index: Int, time: Int) {
    day_time[index] = min(time, MAX_TIME)
    editor!!.putInt("day_time_$index", day_time[index])
    editor!!.apply()
    sendCommandDayTime(index, day_time[index])
}

fun getDayTime(index: Int): Int {
    day_time[index] = settings!!.getInt("day_time_$index", day_time[index]);
    return day_time[index]
}

fun setDayLength(index: Int, length: Int) {
    day_length[index] = min(length, MAX_TIME)
    day_length[index] = max(length, MIN_LENGTH)

    editor!!.putInt("day_length_$index", day_length[index])
    editor!!.apply()
    sendCommandDayLength(index, day_length[index])
}

fun getDayLength(index: Int): Int {
    day_length[index] = settings!!.getInt("day_length_$index", day_length[index]);
    return day_length[index]
}

// Settings Screen //
fun getIP(): String {
    ip = settings!!.getString("ip", ip)!!;
    return ip
}

fun setIP(_ip: String, _do_reconnect: Boolean = true) {
    var do_reconnect = _do_reconnect && _ip != getIP()

    ip = _ip
    editor!!.putString("ip", ip)
    editor!!.apply()

    if(do_reconnect) reconnect()
}

fun getIPPort() : String {
    port = settings!!.getString("port", port)!!;
    return port
}

fun setIPPort(_port: String, _do_reconnect: Boolean = true) {
    var do_reconnect = _do_reconnect && _port != getIPPort()

    port = _port
    editor!!.putString("port", port)
    editor!!.apply()

    if(do_reconnect) reconnect()
}