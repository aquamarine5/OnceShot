package org.aquarngd.onceshot

import android.view.View

data class ScreenshotEventData(val id:String,val view: View,val closeRunnable: Runnable,val timeoutRunnable: Runnable)
