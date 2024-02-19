package org.aquarngd.onceshot

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.aquarngd.onceshot.ui.theme.OnceShotTheme
import org.aquarngd.stackbricks.StackbricksCompose
import org.aquarngd.stackbricks.WeiboCommentsMsgPvder


class MainActivity : ComponentActivity() {

    companion object {
        const val REQUEST_PERMISSION_NOF = 1001
        const val REQUEST_PERMISSION_IMAGE = 1002
        const val SPKEY_DURATION = "duration"
        const val SPNAME = "onceshot"
        const val classTag = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Intent(this, ForegroundService::class.java).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(classTag, "Call startForegroundService")
                startForegroundService(this)
            } else {
                startService(this)
            }
        }
        setContent {
            drawMainContent()
        }
    }

    @SuppressLint("BatteryLife")
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
                    CreateCard(
                        icon = painterResource(id = R.drawable.icon_test),
                        title = "OnceShot 仍在测试当中",
                        text = "Build version: 第 103 次测试",
                        color = Color.Yellow
                    )
                    StackbricksCompose(
                        rememberCoroutineScope(),
                        LocalContext.current, WeiboCommentsMsgPvder.MsgPvderID, "5001248562483153"
                    ).DrawCompose()
                    drawDurationSettingCard()
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
                    }

                    if (!Settings.canDrawOverlays(this@MainActivity)) {
                        CreateCardButton(
                            onClick = {
                                requestOverlayDisplayPermission()
                            },
                            icon = painterResource(id = R.drawable.icon_floating_window),
                            title = stringResource(R.string.mainwindow_requirepermission_floating_title),
                            text = stringResource(R.string.mainwindow_requirepermission_floating_text),
                            color = colorResource(id = R.color.red_zhuhong)
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
                                color = colorResource(id = R.color.red_zhuhong)
                            )
                            checkPermissionPassed = true
                        }
                        if (applicationContext.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                            CreateCardButton(
                                onClick = {
                                    requestPermissions(
                                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                                        REQUEST_PERMISSION_NOF
                                    )
                                },
                                icon = painterResource(id = R.drawable.icon_notification),
                                title = stringResource(R.string.mainwindow_requirepermission_notification_title),
                                text = stringResource(R.string.mainwindow_requirepermission_notification_text),
                                color = colorResource(id = R.color.red_zhuhong)
                            )
                            checkPermissionPassed = true
                        }
                    }
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
                        if (!Environment.isExternalStorageManager()) {
                            CreateCardButton(
                                onClick = {
                                    startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    })
                                },
                                icon = painterResource(id = R.drawable.icon_file_access),
                                title = stringResource(R.string.mainwindow_requirepermission_fileaccess_title),
                                text = stringResource(R.string.mainwindow_requirepermission_fileaccess_text),
                                color = colorResource(id = R.color.red_zhuhong)
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
                                color = colorResource(id = R.color.red_zhuhong)
                            )
                            checkPermissionPassed = true
                        }
                    }
                    if(!(getSystemService(POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)){
                        CreateCardButton(
                            onClick = {
                                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply{
                                    data = Uri.parse("package:$packageName");
                                })
                            },
                            icon = painterResource(id = R.drawable.icon_battery),
                            title = stringResource(R.string.mainwindow_requirepermission_battery_title),
                            text = stringResource(R.string.mainwindow_requirepermission_battery_text),
                            color = colorResource(id = R.color.red_zhuhong)
                        )
                    }
                    if (checkPermissionPassed) {
                        onceShotTitle = "OnceShot 未启动"
                        onceShotText = "请先给与 OnceShot 全部的必须权限，然后退出重进"
                        onceShotBackgroundColor = colorResource(id = R.color.red_zhuhong)
                        //stopService(Intent(LocalContext.current, ForegroundService::class.java))
                    }
                    CreateCard(
                        icon = painterResource(id = R.drawable.icon_material_design),
                        title = stringResource(R.string.mainwindow_onceshot_compose_title),
                        text = stringResource(R.string.mainwindow_onceshot_compose_text),
                        color = colorResource(id = R.color.blue_jiqing)
                    )
                    CreateCard(
                        icon = painterResource(id = R.drawable.onceshot_logo),
                        title = stringResource(R.string.mainwindow_onceshot_creation_title),
                        text = stringResource(R.string.mainwindow_onceshot_creation_text),
                        color = colorResource(id = R.color.blue_jiqing)
                    )
                }
            }
        }
    }
    @Composable
    fun drawDebugVersionCard(){
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(Color.Yellow),

            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
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
                    painter = painterResource(id = R.drawable.icon_test),
                    contentDescription = "",
                    modifier = iconModifier,
                    tint = Color.Unspecified
                )
                Column(
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text("OnceShot 仍在测试当中", fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Card(

                            shape = CutCornerShape(0.dp),colors = CardDefaults.cardColors(colorResource(id = R.color.blue_jiqing))){

                            Text("v1.2 Reanimated",
                                style= TextStyle(color = Color.Yellow),modifier = Modifier.padding(5.dp,2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(" Build version: 3")
                    }
                    Text("Build version: 第 103 次测试")
                }
            }
        }
    }
    @Composable
    fun drawDurationSettingCard() {
        Card(
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .padding(22.dp, 15.dp)
                    .fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.icon_clock),
                    contentDescription = "",
                    modifier = Modifier
                        .padding(10.dp, 0.dp, 20.dp, 0.dp),
                    tint = Color.Unspecified
                )
                Column(
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text("设置\"分享后删除\"操作在分享多久后删除截图", fontWeight = FontWeight.Bold)
                    //
                    var text by remember {
                        mutableStateOf("30"
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround,
                        modifier =Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            modifier=Modifier.width(130.dp),
                            value = text,
                            onValueChange = {
                                            text=it
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Button(onClick = {
                            val sharedPreferences =
                                getSharedPreferences(SPNAME, MODE_PRIVATE).edit()
                            sharedPreferences.putInt(SPKEY_DURATION, text.toInt())
                            sharedPreferences.apply()
                        }) {
                            Text("保存")
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun CreateCard(
        icon: Painter,
        title: String,
        text: String,
        color: Color
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(color),

            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
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

@Preview()
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
                        var onceShotBackgroundColor by mutableStateOf(colorResource(id = R.color.teal_200))
                        val checkPermissionPassed = false
                        CreateCard(
                            icon = painterResource(id = R.drawable.icon_service_start),
                            title = onceShotTitle,
                            text = onceShotText,
                            color = onceShotBackgroundColor
                        )
                        drawDebugVersionCard()
                        StackbricksCompose(
                            rememberCoroutineScope(),
                            LocalContext.current, WeiboCommentsMsgPvder.MsgPvderID, "5001248562483153"
                        ).DrawCompose()
                        drawDurationSettingCard()
                        CreateCardButton(
                            onClick = {
                                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            },
                            icon = painterResource(id = R.drawable.icon_file_access),
                            title = stringResource(R.string.mainwindow_requirepermission_fileaccess_title),
                            text = stringResource(R.string.mainwindow_requirepermission_fileaccess_text),
                            color = colorResource(id = R.color.red_zhuhong)
                        )
                        CreateCard(
                            icon = painterResource(id = R.drawable.icon_material_design),
                            title = stringResource(R.string.mainwindow_onceshot_compose_title),
                            text = stringResource(R.string.mainwindow_onceshot_compose_text),
                            color = colorResource(id = R.color.blue_jiqing)
                        )
                        CreateCard(
                            icon = painterResource(id = R.drawable.onceshot_logo),
                            title = stringResource(R.string.mainwindow_onceshot_creation_title),
                            text = stringResource(R.string.mainwindow_onceshot_creation_text),
                            color = colorResource(id = R.color.blue_jiqing)
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
                            title = stringResource(R.string.mainwindow_requirepermission_mediastore_title),
                            text = stringResource(R.string.mainwindow_requirepermission_mediastore_text),
                            color = colorResource(id = R.color.red_zhuhong)
                        )
                        CreateCardButton(
                            onClick = {
                                requestPermissions(
                                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                                    MainActivity.REQUEST_PERMISSION_IMAGE
                                )
                            },
                            icon = painterResource(id = R.drawable.icon_read_image),
                            title = stringResource(R.string.mainwindow_requirepermission_readimage_title),
                            text = stringResource(R.string.mainwindow_requirepermission_readimage_text),
                            color = colorResource(id = R.color.red_zhuhong)
                        )
                        CreateCardButton(
                            onClick = {
                                requestOverlayDisplayPermission()
                            },
                            icon = painterResource(id = R.drawable.icon_floating_window),
                            title = stringResource(R.string.mainwindow_requirepermission_floating_title),
                            text = stringResource(R.string.mainwindow_requirepermission_floating_text),
                            color = colorResource(id = R.color.red_zhuhong)
                        )
                        if (checkPermissionPassed) {
                            onceShotTitle = "OnceShot 未启动"
                            onceShotText = "请先给与 OnceShot 全部的必须权限，然后退出重进"
                            onceShotBackgroundColor = colorResource(id = R.color.red_zhuhong)
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