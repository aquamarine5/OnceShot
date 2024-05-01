package org.aquarngd.onceshot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.app.NotificationCompat
import com.google.android.material.button.MaterialButton

class ForegroundService : Service() {
    companion object {
        const val classTag = "ForegroundService"
        const val threadName = "org.aquarngd.onceshot.screenshot_observer"
        const val notificationId = "onceshot.notification"
        const val channelId = "notification_channel"
        const val foregroundServiceChannelId = "foreground_channel"
        const val intent_type_id = "intent_extras_data_type"
        const val intent_path_id = "intent_extras_data_path"
        const val intent_uri_id = "intent_extras_data_uri"
        const val INTENT_DEFAULT = 0
        const val INTENT_SHARE_DELETE = 1
        const val INTENT_DELETE_DIRECTLY = 11
        const val INTENT_SHOW_FLOATINGWINDOW = 2
        const val INTENT_CLOSE_FLOATINGWINDOW = 3
    }

    var screenShotListenManager: ScreenShotListenManager? = null
    var isLive = false
    var relativePath: String? = null
    var uri: Uri? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        screenShotListenManager = ScreenShotListenManager.newInstance(this)
        createNotification()
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(classTag, "Received intent: " + intent?.getIntExtra(intent_type_id, INTENT_DEFAULT))
        if (!isLive) {
            isLive = true
            startFileObserver()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Log.w(classTag, "Received onDestroy")
        super.onDestroy()
        isLive = false
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun getAllShareableApplications(): List<ResolveInfo> {
        return applicationContext.packageManager.queryIntentActivities(
            Intent(
                Intent.ACTION_SEND,
                null
            ).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                type = "image/*"
            }, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        )
    }

    private fun renderShareableApplicationsLayout(view: View, infos: List<ResolveInfo>) {
        view.apply {
            removeFloatingWindowButtons(this)
            val shareableLayout = findViewById<LinearLayout>(R.id.share_layout)
            shareableLayout.visibility = View.VISIBLE
            for (i in 0 until 20) {
                val info = infos[i]
                Log.d(
                    classTag,
                    "${info.activityInfo.name} ${info.loadLabel(shareableLayout.context.packageManager)}"
                )
                val button = MaterialButton(
                    ContextThemeWrapper(
                        shareableLayout.context,
                        com.google.android.material.R.style.Widget_Material3_Button_OutlinedButton_Icon
                    ), null,
                    com.google.android.material.R.attr.materialIconButtonOutlinedStyle
                ).apply {
                    icon = info.loadIcon(shareableLayout.context.packageManager)
                    text = info.loadLabel(shareableLayout.context.packageManager)
                    setStrokeColorResource(R.color.teal_700)
                    textSize = 14F
                    setTextColor(0)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                button.setOnClickListener {
                    //onClickShareDeleteButton()
                }
                shareableLayout.addView(button)
            }
        }
    }

    private fun removeFloatingWindowButtons(view: View) {
        view.apply {
            val btnDeleteDirectly = findViewById<MaterialButton>(R.id.btn_delete_directly)
            val btnDeleteShare = findViewById<MaterialButton>(R.id.btn_delete_after_share)
            btnDeleteDirectly.visibility = View.GONE
            btnDeleteShare.visibility = View.GONE
        }
    }

    private fun callFloatingDialogService(id: Long) {
        startService(Intent().apply {
            setClass(applicationContext, FloatingDialogService::class.java)
            putExtra(intent_type_id, INTENT_SHOW_FLOATINGWINDOW)
            putExtra(intent_uri_id, id)
        })
    }
    private fun startFileObserver() {
        screenShotListenManager!!.setListener {
            relativePath = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                it
            ).encodedPath
            uri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                it
            )
            callFloatingDialogService(it)
            Log.d(classTag, "Call screenShotListenManager, uri:$uri")
        }
        screenShotListenManager!!.startListen()
    }

    private fun readImage(id: Long): Bitmap {
        val uri = ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            id
        )
        relativePath = uri.encodedPath
        return if (Build.VERSION.SDK_INT >= 29) {
            Log.d(classTag, id.toString())
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
        } else Bitmap.createBitmap(111, 111, Bitmap.Config.ARGB_8888)
    }

    private fun sendNotification(id: Long) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(notificationId, channelId, NotificationManager.IMPORTANCE_HIGH)
            channel.description = getString(R.string.nof_screenshot_channel)
            manager.createNotificationChannel(channel)
        }
        Log.d(classTag, "relativePath: $relativePath")
        Log.d(classTag, "url: ${uri.toString()}")
        val builder = NotificationCompat.Builder(this, notificationId).apply {
            setSmallIcon(R.drawable.onceshot_logo)
            setContentTitle("path")
            setStyle(NotificationCompat.BigPictureStyle(this).bigPicture(readImage(id)))
            setLargeIcon(readImage(id))
            priority = NotificationCompat.PRIORITY_HIGH
            setContentText("path")
            setWhen(System.currentTimeMillis())
        }
        manager.notify(22454, builder.build())
    }

    private fun createNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationId,
                foregroundServiceChannelId, NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = getString(R.string.nof_channel_description)
            channel.lockscreenVisibility=Notification.VISIBILITY_PRIVATE
            manager.createNotificationChannel(channel)
        }
        val builder = NotificationCompat.Builder(this, notificationId).apply {
            setSmallIcon(R.drawable.onceshot_logo)
            setContentTitle(getString(R.string.nof_title))
            setContentText(getString(R.string.nof_text))

            priority = NotificationCompat.PRIORITY_MIN
            setWhen(System.currentTimeMillis())
            setOngoing(true)
        }

        Log.d(classTag, "Call startForeground")
        startForeground(16451, builder.build())
    }
}