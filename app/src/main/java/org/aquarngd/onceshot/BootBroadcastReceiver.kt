package org.aquarngd.onceshot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootBroadcastReceiver:BroadcastReceiver() {
    companion object{
        const val classTag="BootBroadcastReceiver"
    }
    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent?.action.equals("android.intent.action.BOOT_COMPLETED")){
            Intent(context, ForegroundService::class.java).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Log.d(classTag, "Call startForegroundService")
                    context?.startForegroundService(this)
                } else {
                    context?.startService(this)
                }
            }
        }
    }
}