package com.gadarts.returnfire.systems.player.handlers.movement

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.gadarts.returnfire.components.ComponentsMapper

abstract class GroundVehicleMovementHandler(
    rotationScale: Float,
    forwardForceSize: Float,
    reverseForceSize: Float,
    maxVelocity: Float,
) :
    VehicleMovementHandler(
        rotationScale = rotationScale,
        forwardForceSize = forwardForceSize,
        reverseForceSize = reverseForceSize,
        maxVelocity = maxVelocity
    ) {

    protected open fun idleEngineSound(character: Entity) {
        ComponentsMapper.ambSound.get(character).pitchTarget = 1F
    }

    override fun applyRotation(clockwise: Int, character: Entity) {
        val rigidBody = ComponentsMapper.physics.get(character).rigidBody
        rigidBody.angularFactor = Vector3.Y
    }

    override fun pushForward(rigidBody: btRigidBody, forwardDirection: Int, character: Entity, deltaTime: Float) {
        super.pushForward(rigidBody, forwardDirection, character, deltaTime)

        if (ComponentsMapper.ambSound.has(character)) {
            ComponentsMapper.ambSound.get(character).pitchTarget = 2F
        }
    }

    override fun update(character: Entity, deltaTime: Float) {
        super.update(character, deltaTime)
        if (!ComponentsMapper.turretBase.has(character)) return

        val turretRotating =
            ComponentsMapper.turret.get(ComponentsMapper.turretBase.get(character).turret).turretRotating
        if (turretRotating != 0) {
            val turretRelativeRotation =
                ComponentsMapper.turret.get(ComponentsMapper.turretBase.get(character).turret).turretRelativeRotation
            ComponentsMapper.turret.get(ComponentsMapper.turretBase.get(character).turret).turretRelativeRotation =
                (turretRelativeRotation + turretRotating * 0.75F * deltaTime * 60F) % 360F
        }
    }
}
