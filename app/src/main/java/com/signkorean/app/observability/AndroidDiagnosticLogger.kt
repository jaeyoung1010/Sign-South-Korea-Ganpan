package com.signkorean.app.observability

import android.util.Log
import com.signkorean.domain.port.output.DiagnosticLogLevel
import com.signkorean.domain.port.output.DiagnosticLoggerPort

/** Local diagnostics only; release builds intentionally keep this adapter disabled. */
class AndroidDiagnosticLogger(
    private val enabled: Boolean,
    private val tag: String = "SignKorean",
) : DiagnosticLoggerPort {
    override fun log(
        level: DiagnosticLogLevel,
        message: String,
        attributes: Map<String, String>,
    ) {
        if (!enabled) return
        val rendered = if (attributes.isEmpty()) message else "$message $attributes"
        when (level) {
            DiagnosticLogLevel.DEBUG -> Log.d(tag, rendered)
            DiagnosticLogLevel.INFO -> Log.i(tag, rendered)
            DiagnosticLogLevel.WARN -> Log.w(tag, rendered)
            DiagnosticLogLevel.ERROR -> Log.e(tag, rendered)
        }
    }
}
