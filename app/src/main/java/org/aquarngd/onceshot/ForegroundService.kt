package org.aquarngd.onceshot

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.BitmapCompat

class ForegroundService: Service() {
    companion object{
        const val classTag="ForegroundService"
        const val threadName="org.aquarngd.onceshot.screenshot_observer"
        const val notificationId="org.aqaurngd.onceshot.notification"
        const val channelId="org.aquarngd.onceshot.notification_channel"
        const val foregroundServiceChannelId="org.aquarngd.onceshot.foreground_channel"
        const val intent_type_id="intent_extras_data.type"
        const val intent_path_id="intent_extras_data.path"
        const val INTENT_DEFAULT=0
        const val INTENT_SHARE_DELETE=1
        const val INTENT_ACTIVITY_DELETE=11
    }

    var screenShotListenManager: ScreenShotListenManager =ScreenShotListenManager.newInstance(this)
    var isLive=false
    var relativePath:String?=null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(!isLive){
            isLive=true
            startFileObserver()
        }
        if(intent!=null) parseIntentAction(intent)
        return super.onStartCommand(intent, flags, startId)
    }
    override fun onDestroy() {
        super.onDestroy()
        isLive=false
        stopForeground(STOP_FOREGROUND_DETACH)
    }
    private fun shareImage(){
        applicationContext.startActivity(Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM,relativePath)
            type = "image/*"
        },getString(R.string.share_screenshot)))
    }
    private fun deleteImage(){
        application.startActivity(Intent().apply {
            setClass(applicationContext,MediaStoreActivity::class.java)
            putExtra(intent_path_id,relativePath)
            putExtra(intent_type_id, INTENT_ACTIVITY_DELETE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
    private fun parseIntentAction(intent:Intent){
        when(intent.getIntExtra(intent_type_id, INTENT_DEFAULT)){
            INTENT_SHARE_DELETE->{
                if(relativePath==null){
                    Log.w(classTag,"relativePath is null")
                }else{
                    //shareImage()
                    deleteImage()
                }
            }
            INTENT_DEFAULT->{

            }
        }

    }
    private fun startFileObserver(){
        screenShotListenManager.setListener {
            sendNotification(it)
            relativePath=ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                it).path
        }
        screenShotListenManager.startListen()
    }
    private fun readImage(id:Long): Bitmap {
        if(Build.VERSION.SDK_INT>=29){
            Log.d(classTag, id.toString())
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id)))
        }
        else return Bitmap.createBitmap(111,111,Bitmap.Config.ARGB_8888);
    }
    private fun sendNotification(id:Long){
        val manager=getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            val channel=NotificationChannel(notificationId,channelId,NotificationManager.IMPORTANCE_HIGH)
            channel.description=getString(R.string.nof_screenshot_channel)
            manager.createNotificationChannel(channel)
        }
        val builder=NotificationCompat.Builder(this,notificationId).apply {
            setSmallIcon(R.drawable.onceshot_logo)
            setContentTitle("path")
            setStyle(NotificationCompat.BigPictureStyle(this).bigPicture(readImage(id)))
            addAction(NotificationCompat.Action(R.drawable.stackbricks_logo,getString(R.string.nof_button_sharedelete)
                ,PendingIntent.getActivity(applicationContext,0,Intent().apply {
                    setClass(applicationContext,MediaStoreActivity::class.java)
                    putExtra(intent_type_id, INTENT_ACTIVITY_DELETE)
            },PendingIntent.FLAG_IMMUTABLE)))
            setContentText("path")
            setWhen(System.currentTimeMillis())
        }
        manager.notify(22,builder.build())
    }
    private fun createNotification(){
        val manager=getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            val channel=NotificationChannel(notificationId,
                foregroundServiceChannelId,NotificationManager.IMPORTANCE_HIGH)
            channel.description=getString(R.string.nof_channel_description)
            manager.createNotificationChannel(channel)
        }
        val builder=NotificationCompat.Builder(this,notificationId).apply {
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentTitle(getString(R.string.nof_title))
            setContentText(getString(R.string.nof_text))
            setWhen(System.currentTimeMillis())
            setOngoing(true)
        }

        startForeground(11,builder.build())
    }
}