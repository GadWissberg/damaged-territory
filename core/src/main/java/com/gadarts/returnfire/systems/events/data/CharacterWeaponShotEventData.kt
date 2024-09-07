package com.gadarts.returnfire.systems.events.data

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Matrix4

object CharacterWeaponShotEventData {
    val direction = Matrix4()
    lateinit var shooter: Entity
        private set

    fun set(shooter: Entity, direction: Matrix4) {
        this.shooter = shooter
        this.direction.set(direction)
    }


}
