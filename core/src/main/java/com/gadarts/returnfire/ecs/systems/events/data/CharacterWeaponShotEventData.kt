package com.gadarts.returnfire.ecs.systems.events.data

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Matrix4

object CharacterWeaponShotEventData {
    var aimSky: Boolean = false
    var target: Entity? = null
        private set
    val direction = Matrix4()
    lateinit var shooter: Entity
        private set

    fun setWithDirection(shooter: Entity, direction: Matrix4?, aimSky: Boolean = false) {
        if (direction != null) {
            this.direction.set(direction)
        }
        this.target = null
        this.shooter = shooter
        this.aimSky = aimSky
    }

    fun setWithTarget(shooter: Entity, target: Entity) {
        this.target = target
        this.shooter = shooter
        this.aimSky = false
    }

}
