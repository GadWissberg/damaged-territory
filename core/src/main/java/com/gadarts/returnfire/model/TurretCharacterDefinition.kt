package com.gadarts.returnfire.model

import com.gadarts.returnfire.assets.definitions.ModelDefinition

enum class TurretCharacterDefinition(
    private val baseModelDefinition: ModelDefinition,
    val turretModelDefinition: ModelDefinition,
) : CharacterDefinition {
    TURRET_CANNON(ModelDefinition.TURRET_BASE, ModelDefinition.TURRET_CANNON);

    override fun getModelDefinition(): ModelDefinition {
        return baseModelDefinition
    }

    override fun getName(): String {
        return name
    }

    override fun getCharacterType(): CharacterType {
        return CharacterType.TURRET
    }

    override fun isRandomizeRotation(): Boolean {
        return false
    }

    override fun isRandomizeScale(): Boolean {
        return false
    }
}
