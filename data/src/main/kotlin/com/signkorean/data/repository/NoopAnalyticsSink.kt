package com.signkorean.data.repository

import com.signkorean.domain.model.AppStartupEvent
import com.signkorean.domain.port.output.AnalyticsSink

/** Production-safe adapter until an opt-in analytics transport is introduced. */
class NoopAnalyticsSink : AnalyticsSink {
    override fun recordAppStarted(event: AppStartupEvent) = Unit
}
