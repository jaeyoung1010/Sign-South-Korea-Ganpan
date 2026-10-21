package com.signkorean.domain.port.output

enum class DiagnosticLogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
}

/**
 * Port for diagnostics that are safe to emit outside the domain.
 * Implementations are responsible for routing and final redaction.
 */
interface DiagnosticLoggerPort {
    fun log(
        level: DiagnosticLogLevel,
        message: String,
        attributes: Map<String, String> = emptyMap(),
    )
}
