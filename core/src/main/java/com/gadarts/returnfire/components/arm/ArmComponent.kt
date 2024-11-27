package com.gadarts.returnfire.components.arm

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.bullet.BulletBehavior

abstract class ArmComponent(
    val armProperties: ArmProperties,
    val spark: Entity,
    val bulletBehavior: BulletBehavior
) : Component {
    var displaySpark: Long = 0L
    var loaded: Long = 0L

    interface RelativePositionCalculator {
        fun calculate(parent: Entity, output: Vector3): Vector3
    }
}
