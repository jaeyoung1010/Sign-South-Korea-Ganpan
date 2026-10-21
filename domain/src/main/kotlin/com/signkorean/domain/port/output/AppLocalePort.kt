package com.signkorean.domain.port.output

/** Platform boundary for Android per-app locales and their compatibility implementation. */
interface AppLocalePort {
    fun getSupported(): Set<String>

    fun getCurrent(): String

    fun setCurrent(languageTag: String)
}
