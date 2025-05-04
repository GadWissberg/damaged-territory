package com.gadarts.returnfire.components.arm

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.components.bullet.BulletBehavior

abstract class ArmComponent(
    val armProperties: ArmProperties,
    val spark: Entity,
    val bulletBehavior: BulletBehavior
) : Component {
    abstract fun isPrimary(): Boolean
    var displaySpark: Long = 0L

    var loaded: Long = 0L

    @Suppress("SimplifyBooleanWithConstants", "KotlinConstantConditions")
    var ammo = if (GameDebugSettings.FORCE_AMMO < 0) armProperties.ammo else GameDebugSettings.FORCE_AMMO
        private set

    interface RelativePositionCalculator {
        fun calculate(parent: Entity, output: Vector3): Vector3
    }

    fun consumeAmmo() {
        if (ammo == 0) return

        ammo--
    }
}
