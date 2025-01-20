package com.gadarts.returnfire.model

import com.gadarts.returnfire.model.definitions.CharacterDefinition
import com.gadarts.returnfire.model.definitions.SimpleCharacterDefinition
import com.gadarts.returnfire.model.definitions.TurretCharacterDefinition

enum class CharacterType(val values: Array<out CharacterDefinition>) {
    SIMPLE(SimpleCharacterDefinition.entries.toTypedArray()), TURRET(TurretCharacterDefinition.entries.toTypedArray())
}
