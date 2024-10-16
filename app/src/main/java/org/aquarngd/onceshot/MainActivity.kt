package org.aquarngd.onceshot

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
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
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tencent.bugly.crashreport.CrashReport
import org.aquarngd.onceshot.ui.theme.OnceShotTheme
import org.aquarngd.stackbricks.StackbricksCompose
import org.aquarngd.stackbricks.msgpvder.GithubApiMsgPvder
import java.util.Locale
import java.util.UUID


class MainActivity : ComponentActivity() {
    var status = ForegroundServiceStatus.STATUS_INIT
    private val permissionList = mutableStateMapOf(
        Manifest.permission.SYSTEM_ALERT_WINDOW to false,
        "android.permission.READ_MEDIA_IMAGES" to false,
        "android.permission.MANAGE_EXTERNAL_STORAGE" to false,
        "android.permission.MANAGE_MEDIA" to false,
        "android.permission.POST_NOTIFICATIONS" to false,
        Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS to false
    )
    private var permissionCheckCounter = false
    private var onceShotIcon by mutableStateOf(R.drawable.icon_service_start)
    private var onceShotTitle by mutableStateOf("OnceShot 服务已经启动")
    private var onceShotText by mutableStateOf("点击停止")
    private var onceShotBackgroundColor by mutableStateOf(Color(81,196,211))

    companion object {
        const val REQUEST_PERMISSION_NOF = 1001
        const val REQUEST_PERMISSION_IMAGE = 1002
        const val SPKEY_DURATION = "duration"
        const val SPKEY_DEVICEID = "device_id"
        const val SPKEY_BOOT_PERMISSION="boot_permission"
        const val SPNAME = "onceshot"
        const val classTag = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashReport.initCrashReport(
            applicationContext,
            "2d98a3477d",
            true,
            CrashReport.UserStrategy(applicationContext).apply {
                deviceID = getDeviceUniqueId()
            })
        setContent {
            drawMainContent()
        }
    }

    override fun onResume() {
        refreshPermissionCheckContent()
        super.onResume()
    }

    private fun getDeviceUniqueId(): String {
        val sharedPreferences =
            getSharedPreferences(SPNAME, MODE_PRIVATE)
        val value = sharedPreferences.getString(SPKEY_DEVICEID, "")
        return if (value == "") {
            val uuid = UUID.randomUUID().toString().replace("-", "")
            sharedPreferences.edit().putString(SPKEY_DEVICEID, uuid).apply()
            uuid
        } else {
            value!!
        }
    }

    private fun refreshPermissionCheckContent() {
        if (!permissionCheckCounter) permissionCheckCounter = true
        else {
            var checkPermissionStatus = true

            if (Settings.canDrawOverlays(this@MainActivity))
                permissionList[Manifest.permission.SYSTEM_ALERT_WINDOW] = false
            else checkPermissionStatus = false
            if (applicationContext.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED)
                permissionList["android.permission.READ_MEDIA_IMAGES"] = false
            else checkPermissionStatus = false
            if (applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                permissionList["android.permission.POST_NOTIFICATIONS"] = false
            else checkPermissionStatus = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    permissionList[Manifest.permission.MANAGE_EXTERNAL_STORAGE] = true
                } else checkPermissionStatus = false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (MediaStore.canManageMedia(applicationContext)) {
                    permissionList[Manifest.permission.MANAGE_MEDIA] = true
                } else checkPermissionStatus = false
            }

            if (checkPermissionStatus) {
                launchService()
                onceShotIcon=R.drawable.icon_service_start
                onceShotTitle="OnceShot 服务已经启动"
                onceShotText="点击停止"
                onceShotBackgroundColor=Color(getColor(R.color.teal_200))
            } else {
                Toast.makeText(applicationContext, "权限还没有全部授权！", Toast.LENGTH_SHORT).show()
            }

        }
    }

    @SuppressLint("BatteryLife")
    @Composable
    fun drawPermissionCheckContent(): Boolean {
        var checkPermissionPassed = false
        if (!Settings.canDrawOverlays(this@MainActivity)) {
            permissionList[Manifest.permission.SYSTEM_ALERT_WINDOW] = true
            if (permissionList[Manifest.permission.SYSTEM_ALERT_WINDOW] == true) {
                CreateCardButton(
                    onClick = {
                        requestOverlayDisplayPermission()
                    },
                    icon = painterResource(id = R.drawable.icon_floating_window),
                    title = stringResource(R.string.mainwindow_requirepermission_floating_title),
                    text = stringResource(R.string.mainwindow_requirepermission_floating_text),
                    color = colorResource(id = R.color.red_zhuhong)
                )
            }


            checkPermissionPassed = true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (applicationContext.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionList[Manifest.permission.READ_MEDIA_IMAGES] = true
                if (permissionList[Manifest.permission.READ_MEDIA_IMAGES] == true) {
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
                }

                checkPermissionPassed = true
            }
            if (applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionList[Manifest.permission.POST_NOTIFICATIONS] = true
                if (permissionList[Manifest.permission.POST_NOTIFICATIONS] == true) {
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
                }

                checkPermissionPassed = true
            }
        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                permissionList[Manifest.permission.MANAGE_EXTERNAL_STORAGE] = true
                if (permissionList[Manifest.permission.MANAGE_EXTERNAL_STORAGE] == true) {
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
                }

                checkPermissionPassed = true
            }
            if (!MediaStore.canManageMedia(applicationContext)) {
                permissionList[Manifest.permission.MANAGE_MEDIA] = true
                if (
                    permissionList[Manifest.permission.MANAGE_MEDIA] == true) {
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
                }
                checkPermissionPassed = true
            }
        }
        if (!(getSystemService(POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(
                packageName
            )
        ) {
            permissionList[Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS] = true
            if(permissionList[Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS] == true){
                CreateCardButton(
                    onClick = {
                        startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:$packageName")
                        })
                    },
                    icon = painterResource(id = R.drawable.icon_battery),
                    title = stringResource(R.string.mainwindow_requirepermission_battery_title),
                    text = stringResource(R.string.mainwindow_requirepermission_battery_text),
                    color = colorResource(id = R.color.red_zhuhong)
                )
            }
        }
        if(!getSharedPreferences(SPNAME,Context.MODE_PRIVATE).getBoolean(SPKEY_BOOT_PERMISSION,false)){
            CreateCardButton(
                onClick = {
                    var componentName: ComponentName? = null
                    val brand = Build.MANUFACTURER
                    val intent = Intent()
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    when (brand.lowercase(Locale.getDefault())) {
                        "samsung" -> componentName = ComponentName(
                            "com.samsung.android.sm",
                            "com.samsung.android.sm.app.dashboard.SmartManagerDashBoardActivity"
                        )

                        "huawei" ->
                            componentName = ComponentName(
                                "com.huawei.systemmanager",
                                "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"
                            )

                        "xiaomi" -> componentName = ComponentName(
                            "com.miui.securitycenter",
                            "com.miui.permcenter.autostart.AutoStartManagementActivity"
                        )

                        "vivo" ->
                            componentName = ComponentName(
                                "com.iqoo.secure",
                                "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                            )

                        "oppo" ->
                            componentName = ComponentName(
                                "com.coloros.oppoguardelf",
                                "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity"
                            )

                        "yulong", "360" -> componentName = ComponentName(
                            "com.yulong.android.coolsafe",
                            "com.yulong.android.coolsafe.ui.activity.autorun.AutoRunListActivity"
                        )

                        "meizu" -> componentName = ComponentName(
                            "com.meizu.safe",
                            "com.meizu.safe.permission.SmartBGActivity"
                        )

                        "oneplus" -> componentName = ComponentName(
                            "com.oneplus.security",
                            "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                        )

                        "letv" -> {
                            intent.action = "com.letv.android.permissionautoboot"
                            intent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                            intent.data = Uri.fromParts("package", packageName, null)
                        }

                        else -> {
                            intent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                            intent.data = Uri.fromParts("package", packageName, null)
                        }

                    }
                    intent.component = componentName
                    getSharedPreferences(SPNAME,Context.MODE_PRIVATE).edit().putBoolean(
                        SPKEY_BOOT_PERMISSION,true).apply()
                },
                icon = painterResource(id = R.drawable.icon_boot),
                title = "推荐为应用添加自启动权限",
                text = "自启动权限可以保证 OnceShot 的持续后台运行",
                color = colorResource(id = R.color.yellow_youcaihuahuang)
            )
        }
        return checkPermissionPassed
    }

    @SuppressLint("BatteryLife")
    @Composable
    fun drawMainContent() {
        val animatedOnceShotBackgroundColor by animateColorAsState(
            targetValue = onceShotBackgroundColor,
            tween(500), label = "an imatedOnceShotBackgroundColor"
        )
        var checkPermissionPassed = false
        OnceShotTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    CreateCardButton(
                        icon = painterResource(id = onceShotIcon),
                        title = onceShotTitle,
                        text = onceShotText,
                        color = animatedOnceShotBackgroundColor
                    ) {
                        when (status) {
                            ForegroundServiceStatus.STATUS_INIT -> {

                            }

                            ForegroundServiceStatus.STATUS_RUNNING -> {
                                stopService(
                                    Intent(
                                        applicationContext,
                                        ForegroundService::class.java
                                    )
                                )
                                onceShotIcon = R.drawable.icon_service_click
                                onceShotTitle = "OnceShot 未在运行中"
                                onceShotText = "点击启动 OnceShot 服务"
                                onceShotBackgroundColor =
                                    Color(getColor(R.color.yellow_youcaihuahuang))
                                status = ForegroundServiceStatus.STATUS_STOP
                            }

                            ForegroundServiceStatus.STATUS_PERMISSION_MISSING -> {
                                refreshPermissionCheckContent()
                            }

                            ForegroundServiceStatus.STATUS_EXCEPTION -> {
                                Toast.makeText(
                                    applicationContext,
                                    "重新尝试启动 OnceShot 服务中",
                                    Toast.LENGTH_SHORT
                                ).show()
                                if (launchService()) {
                                    onceShotIcon = R.drawable.icon_service_start
                                    onceShotTitle = "OnceShot 服务已经启动"
                                    onceShotText = "点击停止"
                                    onceShotBackgroundColor = Color(getColor(R.color.teal_200))
                                }
                            }

                            ForegroundServiceStatus.STATUS_STOP -> {
                                if (launchService()) {
                                    onceShotIcon = R.drawable.icon_service_start
                                    onceShotTitle = "OnceShot 服务已经启动"
                                    onceShotText = "点击停止"
                                    onceShotBackgroundColor = Color(getColor(R.color.teal_200))
                                }
                            }
                        }
                    }
                    drawDebugVersionCard()
                    checkPermissionPassed = drawPermissionCheckContent()
                    StackbricksCompose(
                        rememberCoroutineScope(),
                        LocalContext.current, GithubApiMsgPvder.MsgPvderID, "aquamarine5/OnceShot"
                    ).DrawCompose()
                    drawDurationSettingCard()
                    drawUsageDataShower()
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        CreateCard(
                            icon = painterResource(id = R.drawable.icon_android),
                            title = stringResource(R.string.mainwindow_androidcompatibility_title),
                            text = stringResource(
                                R.string.mainwindow_androidcompatibility_lowlevel_text,
                                Build.VERSION.SDK_INT
                            ),
                            color = colorResource(id = R.color.yellow_youcaihuahuang)
                        )
                    }
                    CreateCard(
                        icon = painterResource(id = R.drawable.icon_material_design),
                        title = stringResource(R.string.mainwindow_onceshot_compose_title),
                        text = stringResource(R.string.mainwindow_onceshot_compose_text),
                        color = colorResource(id = R.color.blue_jiqing)
                    )
                    CreateCardButton(
                        icon = painterResource(id = R.drawable.onceshot_logo),
                        title = stringResource(R.string.mainwindow_onceshot_creation_title),
                        text = stringResource(R.string.mainwindow_onceshot_creation_text),
                        color = colorResource(id = R.color.blue_jiqing)
                    ){
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/aquamarine5/OnceShot")))
                    }

                }
            }
        }
        if (checkPermissionPassed) {
            onceShotIcon = R.drawable.icon_service_error
            onceShotTitle = "OnceShot 未启动"
            onceShotText = "请先给与 OnceShot 全部的必须权限，然后退出重进"
            onceShotBackgroundColor = colorResource(id = R.color.red_zhuhong)
            status = ForegroundServiceStatus.STATUS_PERMISSION_MISSING
        } else {
            launchService()
            onceShotIcon=R.drawable.icon_service_start
            onceShotTitle="OnceShot 服务已经启动"
            onceShotText="点击停止"
            onceShotBackgroundColor=Color(getColor(R.color.teal_200))
            status = ForegroundServiceStatus.STATUS_RUNNING
        }
    }

    private fun launchService(): Boolean {
        try {
            Intent(this, ForegroundService::class.java).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Log.d(classTag, "Call startForegroundService")
                    startForegroundService(this)
                } else {
                    startService(this)
                }
            }
        } catch (ex: Exception) {
            status = ForegroundServiceStatus.STATUS_EXCEPTION
            return false
        } finally {
            status = ForegroundServiceStatus.STATUS_RUNNING
        }
        return true
    }

    @Composable
    fun drawUsageDataShower() {
        val usageDataList = mutableStateListOf<AnalysisDataClass>()
        val sp = getSharedPreferences("UDCA_SP", Context.MODE_PRIVATE)
        AnalysisService.UPLOAD_USAGE_VALUES.forEach {
            usageDataList.add(
                AnalysisDataClass(
                    it,
                    AnalysisService.USAGE_VALUES_STRING[it] ?: "",
                    sp.getInt(it.key, 0)
                )
            )
        }
        Column {
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
                    val iconModifier = Modifier
                        .padding(10.dp, 0.dp, 20.dp, 0.dp)
                    //.size(35.dp)
                    Icon(
                        painter = painterResource(id = R.drawable.icon_usage_info),
                        contentDescription = "",
                        modifier = iconModifier,
                        tint = Color.Unspecified
                    )
                    Column(
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text("使用情况数据分析", fontWeight = FontWeight.Bold)

                        usageDataList.forEach{
                            Row{
                                Text("${it.str}: ", fontSize = 14.sp)
                                Text(it.value.toString(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        Button(onClick = {
                            for ((index,element) in AnalysisService.UPLOAD_USAGE_VALUES.withIndex()){
                                usageDataList[index] = AnalysisDataClass(
                                    element,
                                    AnalysisService.USAGE_VALUES_STRING[element] ?: "",
                                    sp.getInt(element.key, 0)
                                )
                            }
                        }){
                            Text("刷新")
                        }
                        //Text("OnceShot 收集您的使用数据并每日传输至服务器，请放心，这不会泄露您的个人隐私", modifier = Modifier.padding(0.dp,5.dp), fontSize = 12.sp, lineHeight = 15.sp)

                    }
                }
            }
        }
    }

    @Composable
    fun drawDebugVersionCard() {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(colorResource(id = R.color.yellow_youcaihuahuang)),

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
                    ) {
                        Card(
                            shape = CutCornerShape(0.dp),
                            colors = CardDefaults.cardColors(colorResource(id = R.color.blue_jiqing))
                        ) {
                            Text(
                                "v1.4 Improvement",
                                style = TextStyle(color = Color.Yellow),
                                modifier = Modifier.padding(5.dp, 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        //Text(" Build version: 3")
                    }
                    Text("Build version: 第 417 次测试")
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
                        mutableStateOf(
                            getSharedPreferences(SPNAME, MODE_PRIVATE).getInt(
                                SPKEY_DURATION, 30
                            ).toString()
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = text,
                            modifier = Modifier.width(130.dp),
                            onValueChange = {
                                text = it
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Button(onClick = {
                            val sharedPreferences =
                                getSharedPreferences(SPNAME, MODE_PRIVATE).edit()
                            sharedPreferences.putInt(SPKEY_DURATION, text.toInt())
                            sharedPreferences.apply()
                            Toast.makeText(applicationContext, "保存成功", Toast.LENGTH_SHORT)
                                .show()
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
        icon: Painter,
        title: String,
        text: String,
        color: Color,
        onClick: () -> Unit,
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
                    .padding(0.dp, 0.dp, 20.dp, 0.dp)
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
        // A surface container using the 'background' color from the theme
        Surface(
            //modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val usageDataList = mutableStateListOf<AnalysisDataClass>()
            val sp = a.getSharedPreferences("UDCA_SP", Context.MODE_PRIVATE)
            AnalysisService.UPLOAD_USAGE_VALUES.forEach {
                usageDataList.add(
                    AnalysisDataClass(
                        it,
                        AnalysisService.USAGE_VALUES_STRING[it] ?: "",
                        sp.getInt(it.key, 0)
                    )
                )
            }
            Column {
                a.apply {
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
                            val iconModifier = Modifier
                                .padding(10.dp, 0.dp, 20.dp, 0.dp)
                            //.size(35.dp)
                            Icon(
                                painter = painterResource(id = R.drawable.icon_usage_info),
                                contentDescription = "",
                                modifier = iconModifier,
                                tint = Color.Unspecified
                            )
                            Column(
                                horizontalAlignment = Alignment.Start,
                            ) {
                                Text("使用情况数据分析", fontWeight = FontWeight.Bold)

                                LazyColumn {
                                    items(count = usageDataList.size) {
                                        Text("${usageDataList[it].str}: ${usageDataList[it].value}")
                                    }
                                }

                            }
                        }
                    }
                }
                /*
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
                    LocalContext.current,
                    WeiboCommentsMsgPvder.MsgPvderID,
                    "5001248562483153"
                ).DrawCompose()
                drawDurationSettingCard()
                CreateCardButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        }
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
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            requestPermissions(
                                arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                                MainActivity.REQUEST_PERMISSION_IMAGE
                            )
                        }
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
            */
            }
        }

    }
}