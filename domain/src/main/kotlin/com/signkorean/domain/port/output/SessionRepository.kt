package com.signkorean.domain.port.output

import com.signkorean.domain.model.ActiveLearnSession
import com.signkorean.domain.model.ActiveSession

interface SessionRepository {
    suspend fun hasActiveSession(): Boolean

    suspend fun getActiveSession(): ActiveSession? = null

    suspend fun getActiveLearnSession(): ActiveLearnSession? = null
}
