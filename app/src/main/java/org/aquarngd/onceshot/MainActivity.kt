package org.aquarngd.onceshot

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import org.aquarngd.onceshot.ui.theme.OnceShotTheme

class MainActivity : ComponentActivity() {
    companion object{
        const val REQUEST_PERMISSION_NOF=1001
        const val REQUEST_PERMISSION_IMAGE=1002
    }
    private fun checkPermission(){

        if(Build.VERSION.SDK_INT>Build.VERSION_CODES.R){
            if(!MediaStore.canManageMedia(applicationContext)){
                startActivity(Intent(
                    Settings.ACTION_REQUEST_MANAGE_MEDIA,
                    Uri.parse("package: ${applicationContext.packageName}")
                ))
            }

            if(!Environment.isExternalStorageManager()){
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package: ${applicationContext.packageName}")
                    )
                )
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
            }
        }
        if(applicationContext.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            !=PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_PERMISSION_NOF)
        }

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
                    Greeting("Android")
                }
            }
        }
        checkPermission()
    }
}
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    OnceShotTheme {
        Greeting("Android")
    }
}