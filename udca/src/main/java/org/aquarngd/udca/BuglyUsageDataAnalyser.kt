package org.aquarngd.udca

import com.tencent.bugly.crashreport.CrashReport

class BuglyUsageDataAnalyser {
    fun upload(str:String){
        CrashReport.postCatchedException(BuglyUsageDataStructureException(str))
    }
}