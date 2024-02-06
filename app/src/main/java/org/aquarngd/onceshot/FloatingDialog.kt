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
    }
}