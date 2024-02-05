package org.aquarngd.onceshot

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.aquarngd.onceshot.ui.theme.OnceShotTheme
import org.aquarngd.stackbricks.StackbricksCompose
import org.aquarngd.stackbricks.WeiboCmtsMsgPvder

class MainActivity : ComponentActivity() {

    companion object {
        const val REQUEST_PERMISSION_NOF = 1001
        const val REQUEST_PERMISSION_IMAGE = 1002
        const val TAG = "MainActivity"
    }

    private fun checkPermission() {

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {

            Log.d(TAG, "checkPermission: Check manage media permission")
            if (!MediaStore.canManageMedia(applicationContext)) {
                Log.d(TAG, "checkPermission: Request manage media permission")
                startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_MANAGE_MEDIA,
                        Uri.parse("package: ${applicationContext.packageName}")
                    )
                )
            }

        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_PERMISSION_NOF
                )
            }
            if (applicationContext.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    REQUEST_PERMISSION_IMAGE
                )

                Log.d(TAG, "checkPermission: Request manage media images")
            }
        }
        if (applicationContext.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_PERMISSION_NOF
            )
        }

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Intent(this, ForegroundService::class.java).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(this)
            } else {
                startService(this)
            }
        }
        setContent {
            drawMainContent()
        }
    }
    @Composable
    fun drawMainContent(){
        OnceShotTheme {
            // A surface container using the 'background' color from the theme
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column {
                    CreateCardButton(
                        onClick = { /*TODO*/ },
                        icon = painterResource(id = R.drawable.icon_service_start),
                        title = "OnceShot 服务已经启动",
                        text = "点击停止",
                        color = Color(getColor(R.color.teal_200))
                    )
                    StackbricksCompose(
                        rememberCoroutineScope(),
                        LocalContext.current, WeiboCmtsMsgPvder.MsgPvderID, "4936409558027888")
                    if (!Settings.canDrawOverlays(this@MainActivity)) {
                        CreateCardButton(
                            onClick = {
                                requestOverlayDisplayPermission()
                            },
                            icon = painterResource(id = R.drawable.icon_floating_window),
                            title = "需要悬浮窗权限",
                            text = "OnceShot 需要添加悬浮窗让用户在截图后进行进一步操作",
                            color = Color.Red
                        )
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (applicationContext.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                            CreateCardButton(
                                onClick = {
                                    requestPermissions(
                                        arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                                        REQUEST_PERMISSION_IMAGE
                                    )
                                },
                                icon = painterResource(id = R.drawable.icon_read_image),
                                title = "需要读取设备内图片权限",
                                text = "OnceShot 需要通过读取设备图片来监听截图操作来显示操作面板",
                                color = Color.Red
                            )

                        }
                    }
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
                        if (!MediaStore.canManageMedia(applicationContext)) {
                            CreateCardButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        startActivity(Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        })
                                    }
                                },
                                icon = painterResource(id = R.drawable.icon_mediastore_access),
                                title = "需要媒体库管理权限",
                                text = "OnceShot 通过对媒体库(MediaStore)的控制权限来删除无用截图",
                                color = Color.Red
                            )
                        }
                    }

                    CreateCardButton(
                        onClick = { /*TODO*/ },
                        icon = painterResource(id = R.drawable.stackbricks_logo),
                        title = "OnceShot 由 Renegade Creation 开发",
                        text = "作者：@aquamarine5 (@海蓝色的咕咕鸽)",
                        color = Color.Blue
                    )
                }
            }
        }
    }
    @Composable
    fun CreateCardButton(
        onClick: () -> Unit,
        icon: Painter,
        title: String,
        text: String,
        color: Color
    ) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(color),
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .padding(7.dp)
                    .fillMaxWidth()
            ) {
                val iconModifier=Modifier
                    .padding(10.dp, 0.dp, 20.dp, 0.dp)
                    //.size(35.dp)
                Icon(
                    painter = icon,
                    contentDescription = "",
                    modifier = iconModifier
                )
                Column(
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(text)
                }
            }
        }
    }

    fun requestOverlayDisplayPermission() {
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
        )
    }
}

@Preview
@Composable
fun GreetingPreview() {
    OnceShotTheme {
        val a = MainActivity()
        OnceShotTheme {
            // A surface container using the 'background' color from the theme
            Surface(
                //modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column {

                    a.CreateCardButton(
                        onClick = { /*TODO*/ },
                        icon = painterResource(id = R.drawable.onceshot_logo),
                        title = "",
                        text = "",
                        color = Color.Blue
                    )
                    a.CreateCardButton(
                        onClick = { a.requestOverlayDisplayPermission() },
                        icon = painterResource(id = R.drawable.icon_floating_window),
                        title = "需要悬浮窗权限",
                        text = "OnceShot 需要添加悬浮窗让用户在截图后进行进一步操作",

                        color = Color.Red
                    )

                    a.CreateCardButton(
                            onClick = {

                            },
                    icon = painterResource(id = R.drawable.icon_mediastore_access),
                    title = "需要媒体库管理权限",
                    text = "OnceShot 通过对媒体库(MediaStore)的控制权限来删除无用截图",
                    color = Color.Red
                    )
                    a.CreateCardButton(
                            onClick = {

                            },
                    icon = painterResource(id = R.drawable.icon_read_image),
                    title = "需要读取设备内图片权限",
                    text = "OnceShot 需要通过读取设备图片来监听截图操作来显示操作面板",
                    color = Color.Red
                    )

                    a.CreateCardButton(
                            onClick = { /*TODO*/ },
                    icon = painterResource(id = R.drawable.stackbricks_logo),
                    title = "",
                    text = "",
                    color = Color.Blue
                    )
                }
            }


        }
    }
}