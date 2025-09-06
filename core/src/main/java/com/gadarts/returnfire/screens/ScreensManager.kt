package com.gadarts.returnfire.screens

import com.badlogic.gdx.Screen
import com.gadarts.shared.data.definitions.CharacterDefinition

interface ScreensManager {
    fun goToGameplayScreen(selectedCharacter: CharacterDefinition, autoAim: Boolean)
    fun goToHangarScreen()
    fun setScreen(screen: Screen)
    fun setScreenWithFade(screen: Screens, durationInSeconds: Float, param: ScreenSwitchParameters?)
    fun switchScreen(screen: Screens, param: ScreenSwitchParameters?)
}
