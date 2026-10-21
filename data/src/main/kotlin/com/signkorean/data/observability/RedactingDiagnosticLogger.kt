package com.signkorean.data.observability

import com.signkorean.domain.port.output.DiagnosticLogLevel
import com.signkorean.domain.port.output.DiagnosticLoggerPort

/** Final privacy boundary for diagnostics, independent of the logging provider. */
class RedactingDiagnosticLogger(
    private val delegate: DiagnosticLoggerPort,
) : DiagnosticLoggerPort {
    override fun log(
        level: DiagnosticLogLevel,
        message: String,
        attributes: Map<String, String>,
    ) {
        delegate.log(
            level = level,
            message = redactText(message),
            attributes = attributes.mapValues { (key, value) ->
                if (key !in SAFE_ATTRIBUTE_KEYS || SENSITIVE_KEY.matches(key)) REDACTED else redactText(value)
            },
        )
    }

    private fun redactText(value: String): String =
        value
            .replace(DETAILS_ASSIGNMENT_PATTERN) { match ->
                "${match.groupValues[1]}$ASSIGNMENT_SEPARATOR$REDACTED"
            }
            .replace(ASSIGNMENT_PATTERN) { match ->
                "${match.groupValues[1]}$ASSIGNMENT_SEPARATOR$REDACTED"
            }
            .replace(EMAIL_PATTERN, REDACTED)
            .replace(ABSOLUTE_PATH_PATTERN, REDACTED)
            .replace(UUID_PATTERN, REDACTED)

    private companion object {
        const val REDACTED = "<redacted>"
        const val ASSIGNMENT_SEPARATOR = "="
        val SENSITIVE_KEY =
            Regex("(?i)^(?:api[_-]?key|token|secret|authorization|password|user|user[_-]?id|uid|session[_-]?id|details?|report[_-]?details?|path|asset[_-]?uri)$")
        val SAFE_ATTRIBUTE_KEYS =
            setOf(
                "card_id",
                "voice_id",
                "reason",
                "position",
                "session_length",
                "due_count",
                "new_count",
                "category",
                "level",
                "interest_count",
                "onboarding_completed",
                "auto_play",
                "rating",
                "previous_rating",
                "gesture_or_accessibility",
                "got_it_count",
                "review_count",
                "duration_ms",
                "previous_voice",
                "selected_voice",
                "enabled",
            )
        val DETAILS_ASSIGNMENT_PATTERN =
            Regex("(?i)\\b(details?|report[_-]?details?)\\s*[:=]\\s*[^;]+")
        val ASSIGNMENT_PATTERN =
            Regex("(?i)\\b(api[_-]?key|token|secret|authorization|password|user[_-]?id|uid|session[_-]?id|details?|report[_-]?details?|path|asset[_-]?uri)\\s*[:=]\\s*[^,;\\s]+")
        val EMAIL_PATTERN = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
        val ABSOLUTE_PATH_PATTERN = Regex("(?<![A-Za-z0-9])(?:/(?:Users|home|private|var|tmp|data|storage)/[^\\s,;]+|[A-Za-z]:\\\\[^\\s,;]+)")
        val UUID_PATTERN = Regex("(?i)\\b[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\\b")
    }
}
