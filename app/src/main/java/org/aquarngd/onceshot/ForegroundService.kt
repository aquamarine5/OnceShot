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
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationCompat
import androidx.core.graphics.BitmapCompat

class ForegroundService: Service() {
    companion object{
        const val classTag="ForegroundService"
        const val threadName="org.aquarngd.onceshot.screenshot_observer"
        const val notificationId="org.aqaurngd.onceshot.notification"
        const val channelId="org.aquarngd.onceshot.notification_channel"
        const val foregroundServiceChannelId="org.aquarngd.onceshot.foreground_channel"
        const val intent_type_id="intent_extras_data_type"
        const val intent_path_id="intent_extras_data_path"
        const val INTENT_DEFAULT=0
        const val INTENT_SHARE_DELETE=1
        const val INTENT_ACTIVITY_DELETE=11
        const val INTENT_SHOW_FLOATINGWINDOW=2
    }

    var screenShotListenManager: ScreenShotListenManager =ScreenShotListenManager.newInstance(this)
    var isLive=false
    var relativePath:String?=null
    var url:Uri?=null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if(intent!=null){

            when(intent.getIntExtra(intent_type_id, INTENT_DEFAULT)){
                INTENT_SHOW_FLOATINGWINDOW->{
                    Log.d(classTag,"Received show floating window intent.")
                    createFloatingWindow()
                }
                INTENT_DEFAULT->{

                }
            }
        }
        if(intent!=null) Log.d(classTag,isLive.toString())
        if(!isLive){
            isLive=true
            startFileObserver()
        }
        return super.onStartCommand(intent, flags, startId)
    }
    override fun onDestroy() {
        super.onDestroy()
        isLive=false
        stopForeground(STOP_FOREGROUND_DETACH)
    }
    @Deprecated("Use MediaStoreActivity")
    private fun shareImage(){
        Log.d(classTag,relativePath.toString())
        val chooserIntent=Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM,url)
            type = "image/*"
        },getString(R.string.share_screenshot))
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        applicationContext.startActivity(chooserIntent)
        Log.d(classTag,"ShareImage")
    }
    @Deprecated("Use MediaStoreActivity")
    private fun deleteImage(){
        Log.d(classTag,"DeleteImage")
        application.startActivity(Intent().apply {
            setClass(applicationContext,MediaStoreActivity::class.java)
            putExtra(intent_path_id,url.toString())
            putExtra(intent_type_id, INTENT_ACTIVITY_DELETE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
    @Deprecated("Use MediaStoreActivity")
    private fun parseIntentAction(intent:Intent){
        //relativePath=intent.getStringExtra("a")
        when(intent.getIntExtra(intent_type_id, INTENT_DEFAULT)){
            INTENT_SHARE_DELETE->{
                if(relativePath==null){
                    Log.w(classTag,"relativePath is null")
                }else{
                    shareImage()
                    deleteImage()
                }
            }
            INTENT_DEFAULT->{

            }
        }

    }
    @Composable
    fun CreateFloatingWindowUI(){
            Surface(contentColor = Color.White) {
                Column{

                    Text(text = "OnceShot")
                    Button(onClick = { /*TODO*/ }) {
                        Text(stringResource(R.string.btn_DeleteAfterShare))
                    }
                    Button(onClick={/*TODO*/}){
                        Text("直接删除")
                    }
                }
            }

    }
    fun createFloatingWindow(){
        val windowManager=getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val windowParams= WindowManager.LayoutParams().apply {
            gravity= Gravity.START or Gravity.TOP
            x=20
            y=100
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            flags=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE and WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        }
        val inflater= LayoutInflater.from(applicationContext)
        val contentView=LayoutInflater.from(this).inflate(R.layout.activity_floating_dialog,null)
        windowManager.addView(contentView,windowParams)
    }
    private fun startFileObserver(){
        screenShotListenManager.setListener {

            relativePath=ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                it).encodedPath
            url=ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                it)
            startService(Intent().apply {
                setClass(applicationContext,ForegroundService::class.java)
                putExtra(intent_type_id, INTENT_SHOW_FLOATINGWINDOW)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            sendNotification(it)

        }
        screenShotListenManager.startListen()
    }
    private fun readImage(id:Long): Bitmap {
        val uri=ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            id)
        relativePath=uri.encodedPath
        if(Build.VERSION.SDK_INT>=29){
            Log.d(classTag, id.toString())
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
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
        Log.d(classTag,"relativePath: $relativePath")
        Log.d(classTag,"url: ${url.toString()}")
        val builder=NotificationCompat.Builder(this,notificationId).apply {
            setSmallIcon(R.drawable.onceshot_logo)
            setContentTitle("path")
            setStyle(NotificationCompat.BigPictureStyle(this).bigPicture(readImage(id)))
            setLargeIcon(readImage(id))
            priority = NotificationCompat.PRIORITY_HIGH
            setContentIntent(PendingIntent.getActivity(applicationContext,0,Intent().apply {
                    setClass(applicationContext,MediaStoreActivity::class.java)
                    putExtra(intent_path_id,id)
                    putExtra(intent_type_id, MediaStoreActivity.INTENT_SHARE_DELETE)

            },PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
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