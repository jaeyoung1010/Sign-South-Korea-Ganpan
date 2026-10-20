package com.signkorean.domain.port.input

import com.signkorean.domain.model.LearnHomeDashboard

fun interface LoadLearnHome {
    suspend operator fun invoke(): LearnHomeDashboard
}
