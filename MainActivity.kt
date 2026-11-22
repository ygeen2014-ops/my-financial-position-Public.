package com.zone.myfinancialposition

import android.Manifest
import android.app.*
import android.content.*
import android.os.Build
import android.os.Bundle
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
        createChannel()
        val web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.addJavascriptInterface(ReminderBridge(this), "AndroidReminder")
        web.webViewClient = WebViewClient()
        web.loadUrl("file:///android_asset/index.html")
        setContentView(web)
    }
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel("installments", "تنبيهات الأقساط", NotificationManager.IMPORTANCE_HIGH)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
class ReminderBridge(private val context: Context) {
    @JavascriptInterface fun schedule(id: Int, title: String, message: String, millis: Long) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("title", title); putExtra("message", message)
        }
        val pending = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (millis > System.currentTimeMillis()) {
            if (Build.VERSION.SDK_INT >= 23) alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
            else alarm.set(AlarmManager.RTC_WAKEUP, millis, pending)
        }
    }
}
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val n = Notification.Builder(context, "installments")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(intent.getStringExtra("title") ?: "تذكير قسط")
            .setContentText(intent.getStringExtra("message") ?: "لديك قسط مستحق")
            .setAutoCancel(true).build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify((System.currentTimeMillis()%100000).toInt(), n)
    }
}
