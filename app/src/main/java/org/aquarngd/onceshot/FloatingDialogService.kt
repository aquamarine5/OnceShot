package org.aquarngd.onceshot

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Service
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.WindowManager
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButton
import kotlin.math.abs


class FloatingDialogService : Service() {
    companion object {
        const val classTag = "FloatingDialogService"
        const val intent_type_id = "intent_extras_data_type"
        const val intent_path_id = "intent_extras_data_path"
        const val intent_uri_id = "intent_extras_data_uri"
        const val INTENT_DEFAULT = 0
        const val INTENT_SHARE_DELETE = 1
        const val INTENT_DELETE_DIRECTLY = 11
        const val INTENT_SHOW_FLOATINGWINDOW = 2
        const val INTENT_CLOSE_FLOATINGWINDOW = 3
    }

    private var uri: Uri? = null
    private var contentView: View? = null
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(
            classTag, "Received intent: " + intent?.getIntExtra(
                intent_type_id,
                INTENT_DEFAULT
            )
        )
        if (intent != null) {
            val id = intent.getLongExtra(intent_uri_id, -1)
            if (id == -1L) {
                Log.e(classTag, "Id is null!")
            } else {
                uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
            }
            when (intent.getIntExtra(
                intent_type_id,
                INTENT_DEFAULT
            )) {
                INTENT_SHOW_FLOATINGWINDOW -> {
                    Log.d(classTag, "Received show floating window intent.")
                    createFloatingWindow()
                }

                INTENT_DEFAULT -> {
                    Log.w(classTag, "Received default start intent.")
                }

                INTENT_CLOSE_FLOATINGWINDOW -> {
                    closeFloatingWindow()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun closeFloatingWindow() {

        if (contentView != null) {
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.removeView(contentView)
            contentView = null
        }
        stopSelf()
    }

    private fun deleteImage() {
        if (uri == null) {
            Log.e(classTag, "Delete image: Uri is null!")
            return
        }
        val result = contentResolver.delete(uri!!, null, null)
        Log.d(classTag, "Delete image result:${{ result == 1 }}")
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

    private fun onClickShareDeleteButton(contentView: View) {
        shareImage()
        fadeOut(contentView)
        Handler().postDelayed({
            deleteImage()
        }, getDuration()*1000L)
    }
    private fun getDuration():Int{
        return getSharedPreferences(MainActivity.SPNAME, MODE_PRIVATE).getInt(MainActivity.SPKEY_DURATION,30)
    }
    private fun createFloatingWindow() {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val windowParams = WindowManager.LayoutParams().apply {
            gravity = Gravity.START or Gravity.TOP
            x = 20
            y = 100
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            flags = (WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        }
        contentView = LayoutInflater.from(this).inflate(R.layout.activity_floating_dialog, null)
        contentView!!.apply {
            val btnDeleteDirectly = findViewById<MaterialButton>(R.id.btn_delete_directly)
            val btnDeleteShare = findViewById<MaterialButton>(R.id.btn_delete_after_share)
            val btnClose = findViewById<MaterialButton>(R.id.btn_close)
            var lastTouchAction=-1
            var downX=0f
            var xNewSize=0f
            btnClose?.setOnClickListener {
                fadeOut(this)
            }
            btnDeleteDirectly?.setOnClickListener {
                Log.d(classTag, "Call INTENT_DELETE_DIRECTLY")
                deleteImage()
                fadeOut(this)
            }
            btnDeleteShare?.setOnClickListener {
                Log.d(classTag, "Call INTENT_SHARE_DELETE")
                onClickShareDeleteButton(this)
            }
            fadeIn(this)
            setOnTouchListener(OnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        lastTouchAction = MotionEvent.ACTION_DOWN
                        if (downX == 0f) {
                            downX = event.rawX
                        }
                        xNewSize = event.rawX
                        return@OnTouchListener true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (abs(event.rawX - xNewSize) > 5){
                            lastTouchAction = MotionEvent.ACTION_MOVE
                            if (abs(event.rawX - xNewSize) > 15){
                                fadeOut(view)
                                return@OnTouchListener false
                            }
                            //这里给定滑动的位置
                            val moveX = xNewSize - downX
                            if(moveX<=20){
                                view.x=moveX
                            }
                            //记录下最新一个点的位置
                            xNewSize = event.rawX
                            windowManager.updateViewLayout(view, layoutParams)
                        }
                        return@OnTouchListener false
                    }

                    MotionEvent.ACTION_UP -> {
                        if (lastTouchAction == MotionEvent.ACTION_DOWN) { //如果触发了滑动就不是点击事件
                            view.performClick()
                        }else{
                            if (abs(event.rawX - xNewSize) > 15){
                                fadeOut(view)
                            }
                            else{
                                view.animate().apply {
                                    x(20f)
                                    duration= 200
                                }
                            }
                        }
                        return@OnTouchListener false
                    }
                }
                false
            })
        }
        windowManager.addView(contentView, windowParams)
        Log.d(classTag, "Create FloatingDialog successfully.")
    }
    private fun fadeIn(contentView: View){
        contentView.apply {
            findViewById<LinearLayout>(R.id.linear_layout)!!.animate()
                .alpha(1f).duration = 500
        }
    }
    private fun fadeOut(contentView: View){
        contentView.apply {
            findViewById<LinearLayout>(R.id.linear_layout)!!.animate().apply{
                alpha(0f)
                duration=500
                setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        closeFloatingWindow()
                    }
                })
            }
        }
    }
}