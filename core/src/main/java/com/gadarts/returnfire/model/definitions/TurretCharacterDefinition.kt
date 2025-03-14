package com.gadarts.returnfire.model.definitions

import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.model.CharacterType

enum class TurretCharacterDefinition(
    private val hp: Float,
    private val baseModelDefinition: ModelDefinition,
    private val smokeEmissionRelativePosition: Vector3,
    private val gravity: Vector3,
    private val linearFactor: Vector3,
) : CharacterDefinition {
    TURRET_CANNON(40F, ModelDefinition.TURRET_BASE, Vector3(0F, 2F, 0F), Vector3.Zero, Vector3.Zero),
    TANK(100F, ModelDefinition.TANK_BODY, Vector3.Zero, Vector3(0F, -10F, 0F), Vector3(1F, 1F, 1F));

    override fun isFlyer(): Boolean {
        return false
    }

    override fun getModelDefinition(): ModelDefinition {
        return baseModelDefinition
    }

    override fun getMovementHeight(): Float {
        return 0.1F
    }

    override fun getLinearFactor(output: Vector3): Vector3 {
        return output.set(linearFactor)
    }

    override fun getGravity(output: Vector3): Vector3 {
        return output.set(gravity)
    }

    override fun getSmokeEmissionRelativePosition(output: Vector3): Vector3 {
        return output.set(smokeEmissionRelativePosition)
    }

    override fun getHP(): Float {
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

    override fun getScale(): Float {
        return 1F
    }
}
