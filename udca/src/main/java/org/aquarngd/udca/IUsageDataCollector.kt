package org.aquarngd.udca

interface IUsageDataCollector {
    fun collect(key:UsageDataKey)
    fun outputFriendlyDataReport():String
}