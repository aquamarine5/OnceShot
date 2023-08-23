package org.aquarngd.onceshot

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import org.aquarngd.onceshot.ui.theme.OnceShotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Intent(this,ForegroundService::class.java).apply {
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
                startForegroundService(this)
            } else{
                startService(this)
            }
        }
        if(Build.VERSION.SDK_INT>Build.VERSION_CODES.R){
            startActivity(Intent().apply {
                action= Settings.ACTION_REQUEST_MANAGE_MEDIA
            })

            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                )
            )
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