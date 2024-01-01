package org.aquarngd.onceshot

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource

class FloatingDialog : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_floating_dialog)
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
}