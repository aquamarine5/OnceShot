package org.aquarngd.onceshot

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import com.google.android.material.button.MaterialButton

class FloatingDialog : AppCompatActivity() {
    companion object {
        const val classTag = "FloatingDialog"
    }

    private var btnDeleteDirectly: MaterialButton? = null
    private var btnDeleteShare: MaterialButton? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_floating_dialog)
        Log.d(classTag, "Start FloatingDialog")
        btnDeleteDirectly = findViewById(R.id.btn_delete_directly)
        btnDeleteShare = findViewById(R.id.btn_delete_after_share)
        btnDeleteDirectly?.setOnClickListener {
            sendForegroundServiceIntent(ForegroundService.INTENT_DELETE_DIRECTLY)
            Log.d(classTag, "Call ForegroundService INTENT_DELETE_DIRECTLY")
        }
        btnDeleteShare?.setOnClickListener {
            sendForegroundServiceIntent(ForegroundService.INTENT_SHARE_DELETE)
            Log.d(classTag, "Call ForegroundService INTENT_SHARE_DELETE")
        }
    }

    private fun sendForegroundServiceIntent(intentType: Int) {
        startActivity(Intent().apply {
            setClass(applicationContext, ForegroundService::class.java)
            putExtra(ForegroundService.intent_type_id, intentType)
        })
    }
}