package com.gadarts.returnfire.model

enum class CharacterType(val values: Array<out CharacterDefinition>) {
    SIMPLE(SimpleCharacterDefinition.entries.toTypedArray()), TURRET(TurretCharacterDefinition.entries.toTypedArray())
}
