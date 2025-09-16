package com.gadarts.returnfire.screens.transition

import com.gadarts.returnfire.screens.ScreenSwitchParameters
import com.gadarts.returnfire.screens.Screens
import com.gadarts.returnfire.screens.ScreensManager

class TransitionHandler(private val screensManager: ScreensManager) {

    private var screenSwitchParameters: ScreenSwitchParameters? = null
    private var current: Screens? = null
    private var time: Float = 0f
    private var next: Screens? = null
    private var transition: ScreenTransition? = null
    private var phase: Phase = Phase.IDLE

    private enum class Phase { IDLE, FADE_OUT, FADE_IN }

    fun switch(
        screen: Screens,
        duration: Float,
        screenSwitchParameters: ScreenSwitchParameters?
    ) {
        if (next != null) return

        this.next = screen
        this.transition = FadeTransition(duration)
        this.time = 0f
        this.screenSwitchParameters = screenSwitchParameters
        this.phase = Phase.FADE_OUT
    }

    fun render(deltaTime: Float) {
        if (phase == Phase.IDLE || transition == null) return

        time += deltaTime
        val alpha = (time / transition!!.duration).coerceAtMost(1f)

        when (phase) {
            Phase.FADE_OUT -> {
                transition!!.render(alpha, deltaTime)

                if (time >= transition!!.duration) {
                    screensManager.switchScreen(next!!, screenSwitchParameters)
                    current = next
                    next = null
                    phase = Phase.FADE_IN
                    time = 0f
                }
            }

            Phase.FADE_IN -> {
                transition!!.render(1f - alpha, deltaTime)

                if (time >= transition!!.duration) {
                    transition = null
                    phase = Phase.IDLE
                }
            }

            else -> {}
        }
    }
}
