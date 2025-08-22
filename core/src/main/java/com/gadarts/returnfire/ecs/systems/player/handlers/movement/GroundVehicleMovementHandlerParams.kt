package com.gadarts.returnfire.ecs.systems.player.handlers.movement

open class GroundVehicleMovementHandlerParams(
    val rotationScale: Float,
    val forwardForceSize: Float,
    val reverseForceSize: Float,
    val maxVelocity: Float,
    val engineMaxPitch: Float
)
