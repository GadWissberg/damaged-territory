package com.gadarts.returnfire.model.definitions

import com.gadarts.shared.model.definitions.CharacterDefinition
import com.gadarts.shared.model.definitions.SimpleCharacterDefinition
import com.gadarts.shared.model.definitions.TurretCharacterDefinition

object DeployableCharacters {
    val list = listOf<CharacterDefinition>(SimpleCharacterDefinition.APACHE, TurretCharacterDefinition.TANK)
}