package com.gadarts.returnfire.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.shared.data.CharacterColor
import com.gadarts.shared.data.definitions.characters.CharacterDefinition

class CharacterComponent(
    val definition: CharacterDefinition,
    val color: CharacterColor,
    var fuel: Float,
    var hp: Float
) : Component {
    var idleFuelConsumptionTimer: Float = 0.0f
    val creationTime = TimeUtils.millis()
    var smokeEmission: Entity? = null
    var dead: Boolean = false

    fun takeDamage(damage: Float) {
        hp -= damage
    }

}
