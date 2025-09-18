package com.gadarts.returnfire.screens.types.gameplay

import com.gadarts.returnfire.screens.ScreenSwitchParameters
import com.gadarts.shared.data.definitions.characters.CharacterDefinition

data class ToGamePlayScreenSwitchParameters(val selectedVehicle: CharacterDefinition, val autoAim: Boolean) :
    ScreenSwitchParameters
