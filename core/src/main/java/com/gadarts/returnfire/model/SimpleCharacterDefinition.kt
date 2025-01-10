package com.gadarts.returnfire.model

import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.assets.definitions.ModelDefinition

enum class SimpleCharacterDefinition(
    private val hp: Int,
    private val modelDefinition: ModelDefinition,
    private val gravity: Vector3,
    private val linearFactor: Vector3,
    private val startHeight: Float,
    private val flyer: Boolean,
) : CharacterDefinition {
    APACHE(100, ModelDefinition.APACHE, Vector3.Zero, Vector3(1F, 0F, 1F), 3.9F, true);

    override fun isFlyer(): Boolean {
        return flyer
    }

    override fun getLinearFactor(output: Vector3): Vector3 {
        return output.set(linearFactor)
    }

    override fun getStartHeight(): Float {
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

    override fun getHP(): Int {
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
