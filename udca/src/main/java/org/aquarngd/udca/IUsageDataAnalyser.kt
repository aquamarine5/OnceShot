package org.aquarngd.udca

interface IUsageDataAnalyser {
    suspend fun uploadAllData(collector: IUsageDataCollector)
}