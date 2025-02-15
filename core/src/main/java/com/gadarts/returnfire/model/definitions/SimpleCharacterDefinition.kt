package com.gadarts.returnfire.model.definitions

import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.model.CharacterType

enum class SimpleCharacterDefinition(
    private val hp: Float,
    private val modelDefinition: ModelDefinition,
    private val gravity: Vector3,
    private val linearFactor: Vector3,
    private val startHeight: Float,
    private val flyer: Boolean,
) : CharacterDefinition {
    APACHE(1F, ModelDefinition.APACHE, Vector3.Zero, Vector3(1F, 0F, 1F), CharacterDefinition.FLYER_HEIGHT, true);

    override fun isFlyer(): Boolean {
        return flyer
    }

    override fun getLinearFactor(output: Vector3): Vector3 {
        return output.set(linearFactor)
    }

    override fun getMovementHeight(): Float {
        return startHeight
    }

    override fun getGravity(output: Vector3): Vector3 {
        return output.set(gravity)
    }

    override fun getModelDefinition(): ModelDefinition {
        return modelDefinition
    }

    override fun getSmokeEmissionRelativePosition(output: Vector3): Vector3 {
        return Vector3.Zero
    }

    override fun getHP(): Float {
        return hp
    }

    override fun getName(): String {
        return name
    }

    override fun getCharacterType(): CharacterType {
        return CharacterType.SIMPLE
    }

    override fun isRandomizeRotation(): Boolean {
        return false
    }

    override fun isRandomizeScale(): Boolean {
        return false
    }
}
