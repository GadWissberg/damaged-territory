package com.gadarts.shared.model.definitions

import com.badlogic.gdx.math.Vector3
import com.gadarts.shared.assets.definitions.model.ModelDefinition
import com.gadarts.shared.model.CharacterType
import com.gadarts.shared.model.ElementType

interface CharacterDefinition : ElementDefinition {

    override fun getType(): ElementType {
        return ElementType.CHARACTER
    }

    fun getCharacterType(): CharacterType
    fun getHP(): Float
    fun getSmokeEmissionRelativePosition(output: Vector3): Vector3
    fun getGravity(output: Vector3): Vector3
    fun getLinearFactor(output: Vector3): Vector3
    fun getAngularFactor(output: Vector3): Vector3
    fun getMovementHeight(): Float
    fun isFlyer(): Boolean
    fun isGibable(): Boolean
    fun getCorpseModelDefinitions(): List<ModelDefinition>
    fun isConsumingFuelOnIdle(): Boolean
    fun getFuelConsumptionPace(): Float
    fun isUseSeparateTransformObjectForPhysics(): Boolean
    fun getLinearDamping(): Float
    fun getAngularDamping(): Float
    fun getFriction(): Float

    companion object {
        const val FLYER_HEIGHT = 3.9F
    }
}
