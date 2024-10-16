package org.aquarngd.onceshot

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Service
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import org.aquarngd.udca.SharedPreferenceUsageDataCollector
import org.aquarngd.udca.UsageDataKey
import java.util.UUID


class FloatingDialogService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var dataCollector:SharedPreferenceUsageDataCollector?=null
    private val analysisService=AnalysisService()
    private val eventMap= mutableMapOf<String,ScreenshotEventData>()

    companion object {
        const val classTag = "FloatingDialogService"
        const val INTENT_EXTRAS_TYPE = "intent_extras_data_type"
        const val INTENT_EXTRAS_PATH = "intent_extras_data_path"
        const val INTENT_EXTRAS_URI = "intent_extras_data_uri"
        const val INTENT_EXTRAS_EVENTID = "intent_extras_data_event_id"
        const val INTENT_DEFAULT = 0
        const val INTENT_SHARE_DELETE = 1
        const val INTENT_DELETE_DIRECTLY = 11
        const val INTENT_SHOW_FLOATINGWINDOW = 2
        //const val INTENT_CLOSE_FLOATINGWINDOW = 3
    }

    private var uri: Uri? = null

    //private var contentView: View? = null
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(
            classTag, "Received intent: " + intent?.getIntExtra(
                INTENT_EXTRAS_TYPE,
                INTENT_DEFAULT
            )
        )
        if (intent != null) {
            dataCollector= SharedPreferenceUsageDataCollector(applicationContext)
            val id = intent.getLongExtra(INTENT_EXTRAS_URI, -1)
            if (id == -1L) {
                Log.e(classTag, "Id is null!")
            } else {
                uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
            }
            when (intent.getIntExtra(
                INTENT_EXTRAS_TYPE,
                INTENT_DEFAULT
            )) {
                INTENT_SHOW_FLOATINGWINDOW -> {
                    Log.d(classTag, "Received show floating window intent.")
                    createFloatingWindow()
                }

                INTENT_DEFAULT -> {
                    Log.w(classTag, "Received default start intent.")
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun closeFloatingWindow(view: View) {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.removeView(view)
        Log.d(classTag,"removeView")
        stopSelf()
    }

    private fun deleteImage() {
        if (uri == null) {
            Log.e(classTag, "Delete image: Uri is null!")
            return
        }
        val result = contentResolver.delete(uri!!, null, null)
        Log.d(classTag, "Delete image result:${{ (result == 1).toString() }}")
    }

    private fun shareImage() {
        startActivity(
            Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "image/*"
            }, "Share screenshot").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
    }

    private fun onClickShareDeleteButton(view: View,id:String) {
        shareImage()
        fadeOut(view,id)
        Handler(Looper.getMainLooper()).postDelayed({
            deleteImage()
        }, getDuration() * 1000L)
    }

    private fun animSlide(
        view: View,
        leftFrom: Int,
        leftTo: Int,
        duration: Int,
        windowManager: WindowManager,
        lp: WindowManager.LayoutParams
    ) {
        val valueAnimator = ValueAnimator.ofInt(leftFrom, leftTo)
        valueAnimator.addUpdateListener { _ ->
            val viewLeft = valueAnimator.animatedValue as Int
            lp.x = viewLeft
            windowManager.updateViewLayout(view, lp)
        }
        valueAnimator.duration = (if (duration < 0) 0 else duration).toLong()
        valueAnimator.start()
    }

    private fun getDuration(): Int {
        return getSharedPreferences(
            MainActivity.SPNAME,
            MODE_PRIVATE
        ).getInt(MainActivity.SPKEY_DURATION, 30)
    }

    private fun createFloatingWindow() {
        val id=UUID.randomUUID().toString()
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val windowParams = WindowManager.LayoutParams().apply {
            gravity = Gravity.START or Gravity.TOP
            x = -50
            y = 100
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            flags = (WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            format = PixelFormat.TRANSPARENT
        }
        val view = LayoutInflater.from(this).inflate(R.layout.activity_floating_dialog, null)
        view!!.apply {
            var lastTouchAction = -1
            var clickX = 0f
            var clickY = 0f
            var x = 0f
            var y = 0f
            val onTouchListener = OnTouchListener { view, event ->
                val lp = layoutParams as WindowManager.LayoutParams
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        lastTouchAction = MotionEvent.ACTION_DOWN
                        clickX = event.rawX
                        clickY = event.rawY
                        x = event.rawX
                        y = event.rawY
                        return@OnTouchListener false
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val nowX = event.rawX
                        val nowY = event.rawY
                        val movedX = nowX - x
                        val movedY = nowY - y
                        lastTouchAction = MotionEvent.ACTION_MOVE
                        x = nowX
                        y = nowY
                        lp.x += movedX.toInt()
                        lp.y += movedY.toInt()
                        windowManager.updateViewLayout(this, lp)
                        return@OnTouchListener false
                    }

                    MotionEvent.ACTION_UP -> {
                        if (lastTouchAction == MotionEvent.ACTION_DOWN) {
                            Log.d(classTag, "performClick because of ACTION_DOWN")
                            //view.performClick()

                            return@OnTouchListener false
                        } else {
                            if (kotlin.math.abs((clickX * clickX - event.rawX * event.rawX) + (clickY * clickY - event.rawY * event.rawY)) < 400) {
                                Log.d(
                                    classTag,
                                    "performClick because of distance < 400, ${(clickX * clickX - event.rawX * event.rawX) + (clickY * clickY - event.rawY * event.rawY)}"
                                )
                                //view.performClick()
                                return@OnTouchListener false
                            }
                            if ((event.rawX - 20) < -20) {
                                fadeOutSlided(this,id)
                                windowManager.updateViewLayout(this, lp)
                                return@OnTouchListener true
                            } else {
                                val limitDistance =
                                    (resources.displayMetrics.widthPixels - this.width) / 2
                                if (this.x < limitDistance) {
                                    animSlide(
                                        this,
                                        lp.x,
                                        20,
                                        (1000 * (this.left + 20) / (limitDistance + 20)),
                                        windowManager,
                                        lp
                                    )
                                } else {
                                    animSlide(
                                        this,
                                        lp.x,
                                        resources.displayMetrics.widthPixels - 20 - this.width,
                                        (1000 * (resources.displayMetrics.widthPixels - this.right) / limitDistance),
                                        windowManager,
                                        lp
                                    )
                                }
                                windowManager.updateViewLayout(this, lp)
                            }
                            return@OnTouchListener true
                        }
                    }
                }
                false
            }
            val btnDeleteDirectly = findViewById<MaterialButton>(R.id.btn_delete_directly)
            val btnDeleteShare = findViewById<MaterialButton>(R.id.btn_delete_after_share)
            val btnClose = findViewById<MaterialButton>(R.id.btn_close)
            val btnWaiting = findViewById<MaterialButton>(R.id.btn_waiting)
            val btnOk = findViewById<MaterialButton>(R.id.btn_waiting_ok)
            val btnIgnore = findViewById<MaterialButton>(R.id.btn_waiting_ignore)
            btnWaiting?.setOnClickListener {
                onClickWaitingButton(view,id)
            }
            btnClose?.setOnClickListener {
                fadeOut(this,id)
                collectUsageData(AnalysisDataKey.CLICK_CLOSE)
            }
            btnDeleteDirectly?.setOnClickListener {
                Log.d(classTag, "Call INTENT_DELETE_DIRECTLY")
                deleteImage()
                fadeOut(this,id)
                collectUsageData(AnalysisDataKey.CLICK_DELETE_DIRECTLY)
            }
            btnDeleteShare?.setOnClickListener {
                Log.d(classTag, "Call INTENT_SHARE_DELETE")
                onClickShareDeleteButton(this,id)
                collectUsageData(AnalysisDataKey.CLICK_DELETE_AFTER_SHARE)
            }
            btnWaiting.setOnTouchListener(onTouchListener)
            btnClose.setOnTouchListener(onTouchListener)
            btnDeleteDirectly.setOnTouchListener(onTouchListener)
            btnDeleteShare.setOnTouchListener(onTouchListener)
            btnOk.setOnTouchListener(onTouchListener)
            btnIgnore.setOnTouchListener(onTouchListener)
            setOnTouchListener(onTouchListener)
            fadeIn(this)
        }
        windowManager.addView(view, windowParams)
        val closeFloatingDialogRunnable = Runnable {
            fadeOut(view, id)
            collectUsageData(AnalysisDataKey.TIMEOUT_CLOSE)
        }
        val showTipsRunnable = Runnable {
            view.findViewById<MaterialTextView>(R.id.text_title)?.text=getText(R.string.floatingdialog_tips)
            windowManager.updateViewLayout(view, view.layoutParams)
        }
        handler.postDelayed(closeFloatingDialogRunnable, 15 * 1000L)
        handler.postDelayed(showTipsRunnable, 5 * 1000L)
        eventMap[id] = ScreenshotEventData(id,view,closeFloatingDialogRunnable,showTipsRunnable)
        Log.d(classTag, "Create FloatingDialog successfully.")
    }
    private fun collectUsageData(key: UsageDataKey){
        dataCollector?.collect(key)
        analysisService.tryUpload(applicationContext)
        val d=dataCollector?.getSharedPreference()?.getInt(key.key,-1)
        Log.d(classTag,"collect ${key.key} ${dataCollector==null} ${dataCollector?.getSharedPreference()?.getInt(key.key,-1)}")
    }
    private fun removeRunnableCallbacks(id:String){
        if(eventMap[id]!=null){
            handler.removeCallbacks(eventMap[id]!!.closeRunnable)
            handler.removeCallbacks(eventMap[id]!!.timeoutRunnable)
        }
    }
    private fun onClickWaitingButton(view:View,id:String) {
        removeRunnableCallbacks(id)
        clearFloatingDialog(view)
        view.apply {
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val btnOk = findViewById<MaterialButton>(R.id.btn_waiting_ok)
            val btnIgnore = findViewById<MaterialButton>(R.id.btn_waiting_ignore)
            val textTips = findViewById<MaterialTextView>(R.id.text_tips)
            textTips.text = context.getString(R.string.floatingdialog_tips_alreadywaiting)
            btnOk.visibility = View.VISIBLE
            btnIgnore.visibility = View.VISIBLE
            windowManager.updateViewLayout(this, this.layoutParams)
            btnOk.setOnClickListener {
                Log.d(classTag, "Call INTENT_DELETE_DIRECTLY on waiting panel")
                deleteImage()
                fadeOut(this,AnalysisDataKey.CLICK_WAITING_DELETE,id)
            }
            btnIgnore.setOnClickListener {
                fadeOut(this,AnalysisDataKey.CLICK_WAITING_IGNORE,id)
            }
        }
    }

    private fun clearFloatingDialog(view:View) {
        view.apply {
            val btnDeleteDirectly = findViewById<MaterialButton>(R.id.btn_delete_directly)
            val btnDeleteShare = findViewById<MaterialButton>(R.id.btn_delete_after_share)
            val btnClose = findViewById<MaterialButton>(R.id.btn_close)
            val btnWaiting = findViewById<MaterialButton>(R.id.btn_waiting)
            btnWaiting.visibility = View.GONE
            btnDeleteShare.visibility = View.GONE
            btnClose.visibility = View.GONE
            btnDeleteDirectly.visibility = View.GONE
        }
    }

    private fun fadeIn(view: View) {
        view.apply {
            animate()
                .alpha(1f)
                .setInterpolator(DecelerateInterpolator())
                .duration = 200
        }
    }

    private fun fadeOutSlided(view: View,id:String) {
        removeRunnableCallbacks(id)
        view.apply {
            animate().apply {
                alpha(0f)
                x(-150f)
                interpolator = DecelerateInterpolator()
                duration = 200
                setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        closeFloatingWindow(view)
                        super.onAnimationEnd(animation)
                    }
                })
            }
        }
    }

    private fun fadeOut(view: View,id:String) {
        view.apply {
            findViewById<LinearLayout>(R.id.linear_layout)!!.animate().apply {
                alpha(0f)
                interpolator = DecelerateInterpolator()
                duration = 200
                setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        closeFloatingWindow(view)
                        super.onAnimationEnd(animation)
                    }
                })
            }
        }
        removeRunnableCallbacks(id)
    }

    private fun fadeOut(view: View,usageDataKey: UsageDataKey,id:String){
        fadeOut(view,id)
        collectUsageData(usageDataKey)
    }
}