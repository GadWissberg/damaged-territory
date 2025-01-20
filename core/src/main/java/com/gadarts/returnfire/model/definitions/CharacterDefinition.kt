package com.gadarts.returnfire.model.definitions

import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.model.CharacterType
import com.gadarts.returnfire.model.ElementType

interface CharacterDefinition : ElementDefinition {
    override fun getType(): ElementType {
        return ElementType.CHARACTER
    }

    fun getCharacterType(): CharacterType
    fun getHP(): Int
    fun getSmokeEmissionRelativePosition(output: Vector3): Vector3
    fun getGravity(output: Vector3): Vector3
    fun getLinearFactor(output: Vector3): Vector3
    fun getStartHeight(): Float
    fun isFlyer(): Boolean
}