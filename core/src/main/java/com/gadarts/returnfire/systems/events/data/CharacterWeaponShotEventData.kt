package com.gadarts.returnfire.systems.events.data

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Matrix4

object CharacterWeaponShotEventData {
    var target: Entity? = null
        private set
    val direction = Matrix4()
    lateinit var shooter: Entity
        private set

    fun setWithDirection(shooter: Entity, direction: Matrix4?, target: Entity?) {
        this.shooter = shooter
        if (direction != null) {
            this.direction.set(direction)
        }
        this.target = target
    }

    fun setWithTarget(shooter: Entity, target: Entity) {
        this.shooter = shooter
        this.target = target
    }

}
