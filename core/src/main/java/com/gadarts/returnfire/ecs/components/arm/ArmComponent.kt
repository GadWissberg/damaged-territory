package com.gadarts.returnfire.ecs.components.arm

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.ecs.components.bullet.BulletBehavior

abstract class ArmComponent(
    val armProperties: ArmProperties,
    val spark: Entity,
    val bulletBehavior: BulletBehavior,
    ammo: Int
) : Component {
    abstract fun isPrimary(): Boolean
    var displaySpark: Long = 0L

    var loaded: Long = 0L
    var ammo = ammo
        private set

    interface RelativePositionCalculator {
        fun calculate(parent: Entity, output: Vector3): Vector3
    }

    fun consumeAmmo() {
        if (ammo == 0) return

        ammo--
    }
}
