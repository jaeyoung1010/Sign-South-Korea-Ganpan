package com.signkorean.data.settings

import android.util.Log

class AndroidSettingsDiagnostics : SettingsDiagnostics {
    override fun onCorruptionRecovered() {
        Log.w(TAG, "Recovered corrupt user settings with defaults.")
    }

    private companion object {
        const val TAG = "UserSettings"
    }
}
