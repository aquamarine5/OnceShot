package org.aquarngd.onceshot

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat

class ForegroundService: Service() {
    companion object{
        const val threadName="org.aquarngd.onceshot.screenshot_observer"
        const val notificationId="org.aqaurngd.onceshot.notification"
        const val channelId="org.aquarngd.onceshot.notification_channel"
        const val foregroundServiceChannelId="org.aquarngd.onceshot.foreground_channel"
    }

    var screenShotListenManager: ScreenShotListenManager =ScreenShotListenManager.newInstance(this)
    var isLive=false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isLive=true
        startFileObserver()
        return super.onStartCommand(intent, flags, startId)
    }
    override fun onDestroy() {
        super.onDestroy()
        isLive=false
        stopForeground(STOP_FOREGROUND_DETACH)
    }

    private fun startFileObserver(){
        screenShotListenManager.setListener {
            sendNotification(it)
        }
        screenShotListenManager.startListen()
    }

    private fun sendNotification(path:String){
        val manager=getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            val channel=NotificationChannel(notificationId,channelId,NotificationManager.IMPORTANCE_HIGH)
            channel.description="Screenshot event"
            manager.createNotificationChannel(channel)
        }
        val builder=NotificationCompat.Builder(this,notificationId).apply {
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentTitle(path)
            setContentText(path)
            setWhen(System.currentTimeMillis())
        }
        manager.notify(22,builder.build())
    }
    private fun createNotification(){
        val manager=getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            val channel=NotificationChannel(notificationId,
                foregroundServiceChannelId,NotificationManager.IMPORTANCE_HIGH)
            channel.description="Foreground"
            manager.createNotificationChannel(channel)
        }
        val builder=NotificationCompat.Builder(this,notificationId).apply {
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentTitle("s")
            setContentText("t")
            setWhen(System.currentTimeMillis())
            setOngoing(true)
        }

        startForeground(11,builder.build())
    }
}