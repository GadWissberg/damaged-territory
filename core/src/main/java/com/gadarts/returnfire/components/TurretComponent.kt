package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector3

class TurretComponent(val base: Entity, val followBase: Boolean, val cannon: Entity?) : Component {
    private val baseOffset = Vector3()
    var baseOffsetApplied = false
    private var currentShootingArm: Int = 1
    var turretRelativeRotation: Float = 0F


    fun updateCurrentShootingArm(): Int {
        this.currentShootingArm *= -1
        return this.currentShootingArm
    }

    fun getBaseOffset(output: Vector3): Vector3 {
        return output.set(baseOffset)
    }

    fun setBaseOffset(offset: Vector3) {
        this.baseOffset.set(offset)
    }


}
