package com.gadarts.shared.model

import com.gadarts.shared.model.definitions.CharacterDefinition
import com.gadarts.shared.model.definitions.SimpleCharacterDefinition
import com.gadarts.shared.model.definitions.TurretCharacterDefinition

enum class CharacterType(val values: Array<out CharacterDefinition>) {
    SIMPLE(SimpleCharacterDefinition.entries.toTypedArray()), TURRET(TurretCharacterDefinition.entries.toTypedArray())
}
