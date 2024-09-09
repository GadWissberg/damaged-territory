package com.gadarts.returnfire.model

import com.badlogic.gdx.math.Vector3

interface CharacterDefinition : ElementDefinition {
    override fun getType(): ElementType {
        return ElementType.CHARACTER
    }

    fun getCharacterType(): CharacterType
    fun getHP(): Int
    fun getSmokeEmissionRelativePosition(output: Vector3): Vector3
}
