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
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import org.aquarngd.stackbricks.WeiboCommentsMsgPvder

class MainActivity : ComponentActivity() {

    companion object {
        const val REQUEST_PERMISSION_NOF = 1001
        const val REQUEST_PERMISSION_IMAGE = 1002
        const val classTag = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Intent(this, ForegroundService::class.java).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(classTag,"Call startForegroundService")
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
    fun drawMainContent() {
        var onceShotTitle by mutableStateOf("OnceShot 服务已经启动")
        var onceShotText by mutableStateOf("点击停止")
        var onceShotBackgroundColor by mutableStateOf(Color(getColor(R.color.teal_200)))
        var checkPermissionPassed = false
        OnceShotTheme {
            // A surface container using the 'background' color from the theme
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    CreateCard(
                        icon = painterResource(id = R.drawable.icon_service_start),
                        title = onceShotTitle,
                        text = onceShotText,
                        color = onceShotBackgroundColor
                    )
                    StackbricksCompose(
                        rememberCoroutineScope(),
                        LocalContext.current, WeiboCommentsMsgPvder.MsgPvderID, "4936409558027888"
                    )
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        CreateCard(
                            icon = painterResource(id = R.drawable.icon_android),
                            title = stringResource(R.string.mainwindow_androidcompatibility_title),
                            text = stringResource(
                                R.string.mainwindow_androidcompatibility_lowlevel_text,
                                Build.VERSION.SDK_INT
                            ),
                            color = Color.Yellow
                        )
                    } else {
                        CreateCard(
                            icon = painterResource(id = R.drawable.icon_android),
                            title = stringResource(R.string.mainwindow_androidcompatibility_title),
                            text = stringResource(
                                R.string.mainwindow_androidcompatibility_success_text,
                                Build.VERSION.SDK_INT
                            ),
                            color = Color.Green
                        )
                    }
                    if (!Settings.canDrawOverlays(this@MainActivity)) {
                        CreateCardButton(
                            onClick = {
                                requestOverlayDisplayPermission()
                            },
                            icon = painterResource(id = R.drawable.icon_floating_window),
                            title = stringResource(R.string.mainwindow_requirepermission_floating_title),
                            text = stringResource(R.string.mainwindow_requirepermission_floating_text),
                            color = Color.Red
                        )
                        checkPermissionPassed = true
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
                                title = stringResource(R.string.mainwindow_requirepermission_readimage_title),
                                text = stringResource(R.string.mainwindow_requirepermission_readimage_text),
                                color = Color.Red
                            )
                        }
                        checkPermissionPassed = true
                    }
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
                        if(!Environment.isExternalStorageManager()){
                            CreateCardButton(
                                onClick = {
                                    startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    })
                                },
                                icon = painterResource(id = R.drawable.icon_mediastore_access),
                                title = stringResource(R.string.mainwindow_requirepermission_fileaccess_title),
                                text = stringResource(R.string.mainwindow_requirepermission_fileaccess_text),
                                color = Color.Red
                            )
                            checkPermissionPassed = true
                        }
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
                                title = stringResource(R.string.mainwindow_requirepermission_mediastore_title),
                                text = stringResource(R.string.mainwindow_requirepermission_mediastore_text),
                                color = Color.Red
                            )
                            checkPermissionPassed = true
                        }
                    }
                    if (checkPermissionPassed) {
                        onceShotTitle = "OnceShot 未启动"
                        onceShotText = "请先给与 OnceShot 全部的必须权限，然后退出重进"
                        onceShotBackgroundColor = Color.Red
                        stopService(Intent(LocalContext.current, ForegroundService::class.java))
                    }
                    CreateCard(
                        icon = painterResource(id = R.drawable.icon_material_design),
                        title = stringResource(R.string.mainwindow_onceshot_compose_title),
                        text = stringResource(R.string.mainwindow_onceshot_compose_text),
                        color = Color.Blue
                    )
                    CreateCard(
                        icon = painterResource(id = R.drawable.stackbricks_logo),
                        title = stringResource(R.string.mainwindow_onceshot_creation_title),
                        text = stringResource(R.string.mainwindow_onceshot_creation_text),
                        color = Color.Blue
                    )
                }
            }
        }
    }
    @Composable
    fun CreateCard(
        icon: Painter,
        title: String,
        text: String,
        color: Color){
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(color),

            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)){
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .padding(22.dp, 15.dp)
                    .fillMaxWidth()
            ) {
                val iconModifier = Modifier
                    .padding(10.dp, 0.dp, 20.dp, 0.dp)
                //.size(35.dp)
                Icon(
                    painter = icon,
                    contentDescription = "",
                    modifier = iconModifier,
                    tint = Color.Unspecified
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
                val iconModifier = Modifier
                    .padding(10.dp, 0.dp, 20.dp, 0.dp)
                //.size(35.dp)
                Icon(
                    painter = icon,
                    contentDescription = "",
                    modifier = iconModifier,
                    tint = Color.Unspecified
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
                    a.apply {
                        var onceShotTitle by mutableStateOf("OnceShot 服务已经启动")
                        var onceShotText by mutableStateOf("点击停止")
                        var onceShotBackgroundColor by mutableStateOf(Color.Green)
                        var checkPermissionPassed = false
                        CreateCard(
                            icon = painterResource(id = R.drawable.icon_service_start),
                            title = onceShotTitle,
                            text = onceShotText,
                            color = onceShotBackgroundColor
                        )
                        StackbricksCompose(
                            rememberCoroutineScope(),
                            LocalContext.current,
                            WeiboCommentsMsgPvder.MsgPvderID,
                            "4936409558027888"
                        )
                        CreateCard(
                                icon = painterResource(id = R.drawable.icon_android),
                        title = stringResource(R.string.mainwindow_androidcompatibility_title),
                        text = stringResource(
                            R.string.mainwindow_androidcompatibility_lowlevel_text,
                            Build.VERSION.SDK_INT
                        ),
                        color = Color.Yellow
                        )
                        CreateCard(
                            icon = painterResource(id = R.drawable.icon_android),
                            title = "OnceShot 目前仅测试了 Android 33 (Tiramisu) 及以上版本的正确使用，其他版本可能会出现问题",
                            text = "您的手机Android版本为${Build.VERSION.SDK_INT}，低于设计版本",
                            color = Color.Yellow
                        )
                        CreateCardButton(
                            onClick = { },
                            icon = painterResource(id = R.drawable.icon_android),
                            title = "OnceShot 目前仅测试了 Android 33 (Tiramisu) 及以上版本的正确使用，其他版本可能会出现问题",
                            text = "您的手机 Android 版本为 ${Build.VERSION.SDK_INT}，可以正常使用",
                            color = Color.Green
                        )

                        CreateCardButton(
                            onClick = {
                                requestOverlayDisplayPermission()
                            },
                            icon = painterResource(id = R.drawable.icon_floating_window),
                            title = "需要悬浮窗权限",
                            text = "OnceShot 需要添加悬浮窗让用户在截图后进行进一步操作",
                            color = Color.Red
                        )
                        CreateCardButton(
                            onClick = {
                                requestPermissions(
                                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                                    MainActivity.REQUEST_PERMISSION_IMAGE
                                )
                            },
                            icon = painterResource(id = R.drawable.icon_read_image),
                            title = "需要读取设备内图片权限",
                            text = "OnceShot 需要通过读取设备图片来监听截图操作来显示操作面板",
                            color = Color.Red
                        )


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


                        if (checkPermissionPassed) {
                            onceShotTitle = "OnceShot 未启动"
                            onceShotText = "请先给与 OnceShot 全部的必须权限，然后退出重进"
                            onceShotBackgroundColor = Color.Red
                            stopService(Intent(LocalContext.current, ForegroundService::class.java))
                        }
                        CreateCardButton(
                            onClick = { },
                            icon = painterResource(id = R.drawable.icon_material_design),
                            title = "OnceShot 使用推荐用于构建原生 Android 界面的新工具包 Jetpack Compose 开发",
                            text = "OnceShot 遵循 Material Design 3 设计理念",
                            color = Color.Blue
                        )
                        CreateCardButton(
                            onClick = { },
                            icon = painterResource(id = R.drawable.stackbricks_logo),
                            title = "OnceShot 由 Renegade Creation 开发",
                            text = "作者：@aquamarine5 (@海蓝色的咕咕鸽)",
                            color = Color.Blue
                        )
                    }
                }
            }


        }
    }
}