package com.gadarts.returnfire.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.shared.data.CharacterColor
import com.gadarts.shared.data.definitions.CharacterDefinition

class CharacterComponent(val definition: CharacterDefinition, val color: CharacterColor) : Component {
    var idleFuelConsumptionTimer: Float = 0.0f
    val creationTime = TimeUtils.millis()
    var smokeEmission: Entity? = null
    var hp: Float = 0F
    var dead: Boolean = false

    @Suppress("KotlinConstantConditions")
    var fuel = if (GameDebugSettings.FORCE_INITIAL_FUEL < 0) 100F else GameDebugSettings.FORCE_INITIAL_FUEL

    init {
        this.hp = definition.getHP()
    }


    fun takeDamage(damage: Float) {
        hp -= damage
    }

}
