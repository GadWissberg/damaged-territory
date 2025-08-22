package com.gadarts.returnfire.model.definitions

import com.gadarts.shared.data.definitions.CharacterDefinition
import com.gadarts.shared.data.definitions.SimpleCharacterDefinition
import com.gadarts.shared.data.definitions.TurretCharacterDefinition

object DeployableCharacters {
    val list = listOf<CharacterDefinition>(SimpleCharacterDefinition.APACHE, TurretCharacterDefinition.TANK)
}
