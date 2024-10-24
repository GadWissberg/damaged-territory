package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.gadarts.returnfire.model.CharacterDefinition

class CharacterComponent(val definition: CharacterDefinition) : Component {
    var smokeEmission: Entity? = null
    var hp: Int = 0
    var dead: Boolean = false
        private set

    init {
        this.hp = definition.getHP()
    }


    fun takeDamage(damage: Int) {
        hp -= damage
    }

    fun die() {
        dead = true
    }

}
