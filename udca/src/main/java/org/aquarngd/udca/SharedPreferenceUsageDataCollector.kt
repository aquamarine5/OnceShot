package org.aquarngd.udca

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor

class SharedPreferenceUsageDataCollector(
    val context: Context,
    val name: String = "UDCA_SP",
    val mode: Int = Context.MODE_PRIVATE
) : IUsageDataCollector {
    fun getSharedPreference(): SharedPreferences {
        return context.getSharedPreferences(name, mode)
    }

    override fun collect(key: UsageDataKey) {
        val sp = getSharedPreference()
        val editor = sp.edit()
        addCount(key,sp,editor)
        editor.apply()
    }
    private fun addCount(key: UsageDataKey,sp: SharedPreferences,editor: Editor){
        editor.putInt(key.key, sp.getInt(key.key, 0)+1)
        key.parentKey?.forEach {
            addCount(it,sp,editor)
        }
    }
    override fun outputFriendlyDataReport(): String {
        TODO("Not yet implemented")
    }

}