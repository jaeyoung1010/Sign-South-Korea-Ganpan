package com.signkorean.domain.port.input

import com.signkorean.domain.model.ProgressSnapshot
import kotlinx.coroutines.flow.Flow

fun interface ObserveProgress {
    operator fun invoke(): Flow<ProgressSnapshot>
}
