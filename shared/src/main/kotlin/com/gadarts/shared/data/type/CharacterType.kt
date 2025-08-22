package com.gadarts.shared.data.type

import com.gadarts.shared.data.definitions.CharacterDefinition
import com.gadarts.shared.data.definitions.SimpleCharacterDefinition
import com.gadarts.shared.data.definitions.TurretCharacterDefinition

enum class CharacterType(val values: Array<out CharacterDefinition>) {
    SIMPLE(SimpleCharacterDefinition.entries.toTypedArray()), TURRET(TurretCharacterDefinition.entries.toTypedArray())
}
