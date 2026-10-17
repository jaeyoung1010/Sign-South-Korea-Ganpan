package com.signkorean.app

internal object BuildContract {
    const val MIN_SUPPORTED_API: Int = 26

    fun supportsApi(apiLevel: Int): Boolean = apiLevel >= MIN_SUPPORTED_API
}
