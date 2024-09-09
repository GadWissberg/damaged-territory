package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Entity
import com.gadarts.returnfire.model.CharacterDefinition

class CharacterComponent : GameComponent() {
    var hp: Int = 0
        private set
    lateinit var definition: CharacterDefinition
        private set
    var emitsSmoke: Entity? = null
    var dead: Boolean = false
        private set

    override fun reset() {
    }

    fun init(definition: CharacterDefinition) {
        this.hp = definition.getHP()
        this.definition = definition
        this.emitsSmoke = null
        this.dead = false
    }

    fun takeDamage(damage: Int) {
        hp -= damage
    }

    fun die() {
        dead = true
    }

}
