package org.aquarngd.onceshot

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview

class FloatingService: Service()
{
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
        val windowParams=WindowManager.LayoutParams().apply {
            gravity=Gravity.START or Gravity.TOP
            x=20
            y=100
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
        }
        val inflater=LayoutInflater.from(applicationContext)
        val contentView=ComposeView(applicationContext).apply {
            setContent {
                CreateFloatingWindowUI()
            }
        }
        windowManager.addView(contentView,windowParams)
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

}
@Preview
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