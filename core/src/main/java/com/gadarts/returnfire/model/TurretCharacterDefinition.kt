package com.gadarts.returnfire.model

import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.assets.definitions.ModelDefinition

enum class TurretCharacterDefinition(
    private val hp: Int,
    private val baseModelDefinition: ModelDefinition,
    private val smokeEmissionRelativePosition: Vector3,
) : CharacterDefinition {
    TURRET_CANNON(20, ModelDefinition.TURRET_BASE, Vector3(0F, 2F, 0F));

    override fun getModelDefinition(): ModelDefinition {
        return baseModelDefinition
    }

    override fun getSmokeEmissionRelativePosition(output: Vector3): Vector3 {
        return smokeEmissionRelativePosition
    }

    override fun getHP(): Int {
        return hp
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
