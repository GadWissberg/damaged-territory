package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.components.character.CharacterColor
import com.gadarts.returnfire.model.definitions.CharacterDefinition

class CharacterComponent(val definition: CharacterDefinition, val color: CharacterColor) : Component {
    val creationTime = TimeUtils.millis()
    var smokeEmission: Entity? = null
    var hp: Float = 0F
    var dead: Boolean = false

    @Suppress("KotlinConstantConditions", "SimplifyBooleanWithConstants")
    var fuel = if (GameDebugSettings.FORCE_INITIAL_FUEL < 0) 100F else GameDebugSettings.FORCE_INITIAL_FUEL

    init {
        this.hp = definition.getHP()
    }


    fun takeDamage(damage: Float) {
        hp -= damage
    }

}
