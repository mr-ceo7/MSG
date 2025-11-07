package com.galvaniytechnologies.MSG.util

import android.content.Context

object DebugConfig {
    private const val PREFS = "msg_prefs"
    private const val KEY_SIMULATE = "simulate_send"

    fun isSimulationEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // Default to true for debug builds so developers don't accidentally send SMS
        val default = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        return prefs.getBoolean(KEY_SIMULATE, default)
    }

    fun setSimulationEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SIMULATE, enabled).apply()
    }
}
