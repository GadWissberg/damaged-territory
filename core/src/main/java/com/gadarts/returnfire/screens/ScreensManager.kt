package com.gadarts.returnfire.screens

interface ScreensManager {
    fun setScreenWithFade(
        screen: Screens,
        durationInSeconds: Float,
        param: ScreenSwitchParameters?
    )

    fun switchScreen(screen: Screens, param: ScreenSwitchParameters?)
}
