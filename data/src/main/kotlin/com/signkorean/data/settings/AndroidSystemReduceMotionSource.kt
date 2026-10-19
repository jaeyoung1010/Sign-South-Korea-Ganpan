package com.signkorean.data.settings

import android.animation.ValueAnimator

class AndroidSystemReduceMotionSource : SystemReduceMotionSource {
    override fun isReduceMotionEnabled(): Boolean = !ValueAnimator.areAnimatorsEnabled()
}
