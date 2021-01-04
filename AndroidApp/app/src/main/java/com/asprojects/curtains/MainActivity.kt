package com.asprojects.curtains

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.StrictMode
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.forEach
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.*


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        initState(this)

        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(R.id.navigation_home, R.id.navigation_schedule, R.id.navigation_settings))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        disableNavBarTooltips(navView)
        displayIconInAppBar()
        reconnect()

        val calendar: Calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 22)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0);

        val intent = Intent(this, NotificationReceiver::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
        val alarmManager = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent)
    }

    fun disableNavBarTooltips(navView: BottomNavigationView) {
        navView.menu.forEach {
            val view = navView.findViewById<View>(it.itemId)
            view.setOnLongClickListener { true }
        }
    }

    fun displayIconInAppBar() {
        val menu: ActionBar? = supportActionBar
        menu?.setDisplayShowHomeEnabled(true)
        menu?.setIcon(ResourcesCompat.getDrawable(resources, R.mipmap.ic_launcher, null))
    }

    fun hideKeyboard(view: View) {
        val imm: InputMethodManager = applicationContext.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        var day: Int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1 // Sunday = 1 so it works out
        if (getIsEnabled() && (getNextDayOverrideEnabled() || getDayEnabled(day))) {
            var notification_time : String
            var notification_length : String

            if (getNextDayOverrideEnabled()) {
                notification_time = getTimeStringFromInt(getNextDayOverrideTime())
                notification_length = getNextDayOverrideLength().toString() + if (getNextDayOverrideLength() == 1) " minute" else " minutes"
            } else {
                notification_time = getTimeStringFromInt(getDayTime(day))
                notification_length = getDayLength(day).toString() + if (getDayLength(day) == 1) " minute" else " minutes"
            }

            val pendingIntent = PendingIntent.getActivity(context, 0, Intent(context.applicationContext, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel("curtains", "Curtains", NotificationManager.IMPORTANCE_HIGH)
            channel.enableLights(true)
            channel.setLightColor(Color.YELLOW)
            notificationManager.createNotificationChannel(channel)

            val builder = NotificationCompat.Builder(context, "curtains")
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle("Curtains")
                .setContentText("Opening scheduled tomorrow at " + notification_time + " over " + notification_length + if (getNextDayOverrideEnabled()) " (overridden)" else "")
                .setContentIntent(pendingIntent)

            notificationManager.notify(0, builder.build())
        }
    }
}