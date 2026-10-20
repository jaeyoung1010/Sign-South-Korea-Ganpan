package com.signkorean.domain.port.output

import com.signkorean.domain.model.AppStartupEvent

fun interface AnalyticsSink {
    fun recordAppStarted(event: AppStartupEvent)
}
