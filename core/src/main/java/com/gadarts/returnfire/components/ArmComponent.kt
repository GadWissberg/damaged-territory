package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.arm.ArmProperties
import com.gadarts.returnfire.components.bullet.BulletBehavior

abstract class ArmComponent(
    val armProperties: ArmProperties,
    val spark: Entity,
    val bulletBehavior: BulletBehavior
) : Component {
    private val relativePosition = Vector3()
    var displaySpark: Long = 0L
    var loaded: Long = 0L

    fun getRelativePosition(output: Vector3): Vector3 {
        return output.set(relativePosition)
    }

    fun setRelativePosition(value: Vector3): Vector3 {
        return relativePosition.set(value)
    }

    interface RelativePositionCalculator {
        fun calculate(parent: Entity, output: Vector3): Vector3
    }
}
