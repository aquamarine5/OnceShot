package org.aquarngd.onceshot

import android.content.Context
import android.content.SharedPreferences
import com.tencent.bugly.crashreport.CrashReport
import org.aquarngd.udca.BuglyUsageDataAnalyser
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AnalysisService {
    companion object{
        const val SETTING_DURATION="sd"
        const val LAST_UPLOAD_TIME="last_upload_time"
        const val UPLOAD_INTERVAL=86400000L
        val UPLOAD_USAGE_VALUES= listOf(
            AnalysisDataKey.SCREENSHOT_COUNT,
            AnalysisDataKey.TIMEOUT_CLOSE,
            AnalysisDataKey.CLICK_CLOSE,
            AnalysisDataKey.CLICK_DELETE_DIRECTLY,
            AnalysisDataKey.CLICK_DELETE_AFTER_SHARE,
            AnalysisDataKey.CLICK_WAITING,
            AnalysisDataKey.CLICK_WAITING_DELETE,
            AnalysisDataKey.CLICK_WAITING_IGNORE
        )
        val USAGE_VALUES_STRING= mapOf(
            AnalysisDataKey.SCREENSHOT_COUNT to "截图次数",
            AnalysisDataKey.TIMEOUT_CLOSE to "悬浮窗自动关闭",
            AnalysisDataKey.CLICK_CLOSE to "点击悬浮窗关闭",
            AnalysisDataKey.CLICK_DELETE_DIRECTLY to "点击直接删除",
            AnalysisDataKey.CLICK_DELETE_AFTER_SHARE to "点击分享后删除",
            AnalysisDataKey.CLICK_WAITING to "点击等等我",
            AnalysisDataKey.CLICK_WAITING_DELETE to "等等我后删除",
            AnalysisDataKey.CLICK_WAITING_IGNORE to "等等我后保留"
        )
    }

    fun tryUpload(context: Context){
        if(checkNeedUpload(context)){
            upload(context)
        }
    }
    fun upload(context: Context){
        val sb=StringBuilder()
        val sp=getSharedPreferences(context)
        UPLOAD_USAGE_VALUES.forEach {
            sb.append("${it.key}:${sp.getInt(it.key,0)}; ")
        }
        sb.append("d: ${sp.getInt(AnalysisDataKey.SETTING_DURATION,30)}")
        BuglyUsageDataAnalyser().upload(sb.toString())
        sp.edit().putLong(LAST_UPLOAD_TIME,System.currentTimeMillis()).apply()
    }
    fun checkNeedUpload(context: Context): Boolean {
        val time=getSharedPreferences(context).getLong(LAST_UPLOAD_TIME,0)
        return System.currentTimeMillis()-time> UPLOAD_INTERVAL
    }
    private fun getSharedPreferences(context: Context):SharedPreferences{
        return context.getSharedPreferences("UDCA_SP",Context.MODE_PRIVATE)
    }
}