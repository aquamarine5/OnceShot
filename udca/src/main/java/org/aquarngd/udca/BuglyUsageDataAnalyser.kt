package org.aquarngd.udca

import android.util.Log
import com.tencent.bugly.crashreport.CrashReport

class BuglyUsageDataAnalyser {
    companion object{
        const val classTag="BuglyUsageDataAnalyser"
    }
    fun upload(str:String){
        CrashReport.postCatchedException(BuglyUsageDataStructureException(str))
        Log.d(classTag,"postCaughtException BuglyUsageDataStructureException")
    }
}