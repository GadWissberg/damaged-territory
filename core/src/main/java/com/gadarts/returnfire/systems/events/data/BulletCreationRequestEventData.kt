package com.gadarts.returnfire.systems.events.data

import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.arm.ArmComponent

object BulletCreationRequestEventData {
    val direction: Matrix4 = Matrix4()
    val relativePosition = Vector3()
    var friendly: Boolean = false
        private set
    lateinit var armComponent: ArmComponent
        private set

    fun set(arm: ArmComponent, friendly: Boolean, relativePosition: Vector3, direction: Matrix4) {
        this.armComponent = arm
        this.friendly = friendly
        this.relativePosition.set(relativePosition)
        this.direction.set(direction)
    }

}
