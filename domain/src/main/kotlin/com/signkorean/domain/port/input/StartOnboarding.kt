package com.signkorean.domain.port.input

/** Input port for the user's explicit transition from the welcome screen to onboarding. */
fun interface StartOnboarding {
    operator fun invoke()
}
