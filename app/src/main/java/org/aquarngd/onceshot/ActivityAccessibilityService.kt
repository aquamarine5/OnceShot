package org.aquarngd.onceshot

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import org.aquarngd.onceshot.ForegroundService.Companion
import org.aquarngd.onceshot.ForegroundService.Companion.INTENT_SHOW_FLOATINGWINDOW
import org.aquarngd.onceshot.ForegroundService.Companion.foregroundServiceChannelId
import org.aquarngd.onceshot.ForegroundService.Companion.intent_type_id
import org.aquarngd.onceshot.ForegroundService.Companion.intent_uri_id
import org.aquarngd.onceshot.ForegroundService.Companion.notificationId

class ActivityAccessibilityService: AccessibilityService() {
    companion object{
        const val classTag="ActivityAccessibilityService"
    }
    private var isLive:Boolean=false
    private var screenShotListenManager: ScreenShotListenManager? = null
    private var relativePath: String? = null
    private var uri: Uri? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        isLive=true
        screenShotListenManager = ScreenShotListenManager.newInstance(this)
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        TODO("Not yet implemented")
    }

    override fun onInterrupt() {
        TODO("Not yet implemented")
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
            startService(Intent().apply {
                setClass(applicationContext, FloatingDialogService::class.java)
                putExtra(intent_type_id, INTENT_SHOW_FLOATINGWINDOW)
                putExtra(intent_uri_id, it)
            })
            Log.d(classTag, "Call screenShotListenManager, uri:$uri")
        }
        screenShotListenManager!!.startListen()
    }
}