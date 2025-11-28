package com.gadarts.returnfire.ecs.systems.events.data

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.ecs.components.arm.ArmComponent
import com.gadarts.shared.data.CharacterColor

object BulletCreationRequestEventData {
    var aimSky: Boolean = false
        private set
    var target: Entity? = null
        private set
    val direction: Matrix4 = Matrix4()
    val relativePosition = Vector3()
    var color: CharacterColor = CharacterColor.BROWN
        private set
    lateinit var armComponent: ArmComponent
        private set

    fun set(
        arm: ArmComponent,
        color: CharacterColor,
        relativePosition: Vector3,
        direction: Matrix4,
        target: Entity?,
        aimSky: Boolean
    ) {
        this.armComponent = arm
        this.color = color
        this.relativePosition.set(relativePosition)
        this.direction.set(direction)
        this.target = target
        this.aimSky = aimSky
    }

}
