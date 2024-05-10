package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.arm.ArmProperties

abstract class ArmComponent : GameComponent() {
    val relativePos = Vector3()
    private lateinit var relativePositionCalculation: CalculateRelativePosition
    lateinit var armProperties: ArmProperties
    var displaySpark: Long = 0L
    lateinit var sparkDecal: Decal
    var loaded: Long = 0L

    fun init(
        sparkDecal: Decal,
        armProperties: ArmProperties,
        relativePositionCalculation: CalculateRelativePosition
    ) {
        loaded = 0L
        displaySpark = 0L
        this.sparkDecal = sparkDecal
        this.armProperties = armProperties
        this.relativePositionCalculation = relativePositionCalculation
    }

    fun calculateRelativePosition(parent: Entity) {
        relativePos.set(relativePositionCalculation.calculate(parent))
    }

    interface CalculateRelativePosition {
        fun calculate(parent: Entity): Vector3
    }
}
