package com.gadarts.returnfire.screens

import com.gadarts.shared.model.definitions.CharacterDefinition

interface ScreensManager {
    fun goToWarScreen(characterDefinition: CharacterDefinition, autoAim: Boolean)
    fun goToHangarScreen()

}
