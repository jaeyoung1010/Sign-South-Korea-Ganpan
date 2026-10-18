package com.signkorean.app.qa

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DebugResetReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == "com.signkorean.app.action.DEBUG_RESET") {
            context
                .getSharedPreferences("debug_qa", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }
    }
}
