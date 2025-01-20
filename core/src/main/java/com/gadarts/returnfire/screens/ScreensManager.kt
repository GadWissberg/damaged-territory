package com.gadarts.returnfire.screens

import com.gadarts.returnfire.model.definitions.CharacterDefinition

interface ScreensManager {
    fun goToWarScreen(characterDefinition: CharacterDefinition, autoAim: Boolean)
    fun goToHangarScreen()

}
