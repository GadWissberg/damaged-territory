package com.gadarts.returnfire.systems.player.handlers.movement.tank

import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.physics.RigidBody
import com.gadarts.returnfire.systems.player.handlers.movement.VehicleMovementHandler

abstract class TankMovementHandler(private val rigidBody: RigidBody) : VehicleMovementHandler(
    -30F,
    4F,
    50F,
    25F
) {
    protected var turretRotating: Int = 0

    override fun applyRotation(clockwise: Int) {
        rigidBody.angularFactor = Vector3.Y
    }

}
