package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.arm.ArmProperties

abstract class ArmComponent : GameComponent() {
    val relativePos = Vector3()
    lateinit var armProperties: ArmProperties
    var displaySpark: Long = 0L
    lateinit var spark: Entity
    var loaded: Long = 0L

    fun init(
        spark: Entity,
        armProperties: ArmProperties,
    ) {
        loaded = 0L
        displaySpark = 0L
        this.spark = spark
        this.armProperties = armProperties
    }

    interface RelativePositionCalculator {
        fun calculate(parent: Entity, output: Vector3): Vector3
    }
}
