package com.signkorean.domain.port.input

import com.signkorean.domain.model.AppInitializationResult

fun interface InitializeApp {
    suspend operator fun invoke(): AppInitializationResult
}
