package com.signkorean.domain.port.input

import com.signkorean.domain.model.LaunchDestination

fun interface ResolveLaunchDestination {
    operator fun invoke(): LaunchDestination
}
