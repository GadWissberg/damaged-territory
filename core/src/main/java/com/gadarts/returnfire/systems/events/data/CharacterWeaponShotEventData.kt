package com.gadarts.returnfire.systems.events.data

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Matrix4

object CharacterWeaponShotEventData {
    var target: Entity? = null
    val direction = Matrix4()
    lateinit var shooter: Entity
        private set

    fun setWithDirection(shooter: Entity, direction: Matrix4) {
        this.shooter = shooter
        this.direction.set(direction)
        this.target = null
    }

    fun setWithTarget(shooter: Entity, target: Entity) {
        this.shooter = shooter
        this.target = target
    }

}
