package com.gadarts.shared.data.type

import com.gadarts.shared.data.definitions.characters.CharacterDefinition
import com.gadarts.shared.data.definitions.characters.SimpleCharacterDefinition
import com.gadarts.shared.data.definitions.characters.TurretCharacterDefinition

enum class CharacterType(val values: Array<out CharacterDefinition>) {
    SIMPLE(SimpleCharacterDefinition.entries.toTypedArray()), TURRET(TurretCharacterDefinition.entries.toTypedArray())
}
