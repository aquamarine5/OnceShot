package org.aquarngd.onceshot

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import org.aquarngd.onceshot.ui.theme.OnceShotTheme

class MainActivity : ComponentActivity() {

    companion object{
        const val REQUEST_PERMISSION_NOF=1001
        const val REQUEST_PERMISSION_IMAGE=1002
        const val TAG="MainActivity"
    }
    private fun checkPermission(){

        if(Build.VERSION.SDK_INT>Build.VERSION_CODES.R){

            Log.d(TAG, "checkPermission: Check manage media permission")
            if(!MediaStore.canManageMedia(applicationContext)){
                Log.d(TAG, "checkPermission: Request manage media permission")
                startActivity(Intent(
                    Settings.ACTION_REQUEST_MANAGE_MEDIA,
                    Uri.parse("package: ${applicationContext.packageName}")
                ))
            }

        }
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU){

            if(applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                !=PackageManager.PERMISSION_GRANTED){
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_PERMISSION_NOF)
            }
            if(applicationContext.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES)
                !=PackageManager.PERMISSION_GRANTED){
                requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    REQUEST_PERMISSION_IMAGE)

                Log.d(TAG, "checkPermission: Request manage media images")
            }
        }
        if(applicationContext.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            !=PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_PERMISSION_NOF)
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
            flags=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        }
        val inflater= LayoutInflater.from(applicationContext)
        val contentView= LayoutInflater.from(this).inflate(R.layout.activity_floating_dialog,null)
        windowManager.addView(contentView,windowParams)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Intent(this,ForegroundService::class.java).apply {
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
                startForegroundService(this)
            } else{
                startService(this)
            }
        }
        setContent {
            OnceShotTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        if(!Settings.canDrawOverlays(this@MainActivity)){
                            CreateCardButton(onClick = { requestOverlayDisplayPermission() }
                                , icon = painterResource(id = R.drawable.floating_window), title = "需要悬浮窗权限", text = "悬浮窗",color= Color.Red)
                        }
                    }


                }
            }
        }
        checkPermission()
        //createFloatingWindow()
    }
    @Composable
    fun CreateCardButton(onClick:()->Unit, icon: Painter,title:String,text:String,color:Color){
        Button(onClick = onClick,
            shape= RoundedCornerShape(18.dp),
            colors= ButtonDefaults.buttonColors(color),
            modifier = Modifier
                .fillMaxWidth()

                .padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .padding(7.dp)
                    .fillMaxWidth()) {

                Icon(painter= icon, contentDescription = "checkOverlayPermission",
                    modifier=Modifier.padding(10.dp,0.dp,20.dp,0.dp) )
                Column(
                    horizontalAlignment = Alignment.Start,
                ){
                    Text(title, fontWeight= FontWeight.Bold)
                    Text(text)
                }
            }
        }
    }

    fun requestOverlayDisplayPermission(){
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")))
    }
}
@Composable
fun GreetingPreview() {
    OnceShotTheme {
        val a=MainActivity()
        OnceShotTheme {
            // A surface container using the 'background' color from the theme
            Surface(
                //modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column {

                    a.CreateCardButton(onClick = { a.requestOverlayDisplayPermission() }
                        , icon = painterResource(id = R.drawable.floating_window), title = "需要悬浮窗权限", text = "悬浮窗",color= Color.Red)

                    a.CreateCardButton(onClick = { a.requestOverlayDisplayPermission() }
                        , icon = painterResource(id = R.drawable.floating_window), title = "需要悬浮窗权限", text = "悬浮窗",color= Color.Red)
                }
            }


        }
    }
}