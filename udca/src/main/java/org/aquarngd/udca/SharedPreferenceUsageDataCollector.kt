package org.aquarngd.udca

import android.content.Context
import android.content.SharedPreferences

class SharedPreferenceUsageDataCollector(
    val context: Context,
    val name: String = "UDCA_SP",
    val mode: Int = Context.MODE_PRIVATE
) : IUsageDataCollector {
    private fun getSharedPreference(): SharedPreferences {
        return context.getSharedPreferences(name, mode)
    }

    override fun collect(key: UsageDataKey) {
        val sp = getSharedPreference()
        val editor = sp.edit()
        key.forEach {
            editor.putInt(key.key, sp.getInt(key.key, 0))
        }
        editor.apply()
    }

    override fun outputFriendlyDataReport(): String {
        TODO("Not yet implemented")
    }

}