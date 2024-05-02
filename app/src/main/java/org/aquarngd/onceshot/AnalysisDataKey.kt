package org.aquarngd.onceshot

import org.aquarngd.udca.UsageDataKey

class AnalysisDataKey {
    companion object{
        val SCREENSHOT_COUNT=UsageDataKey("sc")
        val CLICK_DELETE_DIRECTLY=UsageDataKey("cd", listOf(SCREENSHOT_COUNT))
        val CLICK_CLOSE=UsageDataKey("cc", listOf(SCREENSHOT_COUNT))
        val TIMEOUT_CLOSE=UsageDataKey("tc", listOf(SCREENSHOT_COUNT))
        val CLICK_WAITING=UsageDataKey("cw", listOf(SCREENSHOT_COUNT))
        val CLICK_WAITING_DELETE=UsageDataKey("cwd", listOf(CLICK_WAITING))
        val CLICK_WAITING_IGNORE=UsageDataKey("cwi", listOf(CLICK_WAITING))
        val CLICK_DELETE_AFTER_SHARE=UsageDataKey("ca", listOf(SCREENSHOT_COUNT))
        val SETTING_DURATION="d"
    }
}