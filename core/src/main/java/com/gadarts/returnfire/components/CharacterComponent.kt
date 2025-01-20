package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.components.character.CharacterColor
import com.gadarts.returnfire.model.definitions.CharacterDefinition

class CharacterComponent(val definition: CharacterDefinition, val color: CharacterColor) : Component {
    var deathSequenceNextExplosion: Long = 0
        private set
    var deathSequenceDuration: Int = 0
        private set
    var smokeEmission: Entity? = null
    var hp: Int = 0
    var dead: Boolean = false

    init {
        this.hp = definition.getHP()
        this.deathSequenceDuration = 0
        this.deathSequenceNextExplosion = 0
    }


    fun takeDamage(damage: Int) {
        hp -= damage
    }

    fun beginDeathSequence() {
        deathSequenceDuration = MathUtils.random(2, 4)
        deathSequenceNextExplosion = TimeUtils.millis()
    }

    fun incrementDeathSequence() {
        deathSequenceDuration--
        deathSequenceNextExplosion = TimeUtils.millis() + MathUtils.random(250, 750)
    }

}
