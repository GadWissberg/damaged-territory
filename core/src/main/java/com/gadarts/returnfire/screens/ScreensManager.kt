package com.gadarts.returnfire.screens

import com.gadarts.shared.data.definitions.CharacterDefinition

interface ScreensManager {
    fun goToWarScreen(characterDefinition: CharacterDefinition, autoAim: Boolean)
    fun goToHangarScreen()

}
