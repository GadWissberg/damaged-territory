package com.gadarts.returnfire.ecs.systems.player.handlers.movement

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.gadarts.returnfire.components.ComponentsMapper

abstract class GroundVehicleMovementHandler(private val params: GroundVehicleMovementHandlerParams) :
    VehicleMovementHandler(
        rotationScale = params.rotationScale,
        forwardForceSize = params.forwardForceSize,
        reverseForceSize = params.reverseForceSize,
        maxVelocity = params.maxVelocity
    ) {

    protected open fun idleEngineSound(character: Entity) {
        ComponentsMapper.ambSound.get(character).pitchTarget = 1F
    }

    override fun applyRotation(clockwise: Int, character: Entity) {
        val rigidBody = ComponentsMapper.physics.get(character).rigidBody
        rigidBody.angularFactor = Vector3.Y
    }

    override fun pushForward(
        rigidBody: btRigidBody,
        forwardDirection: Int,
        character: Entity,
        deltaTime: Float
    ) {
        super.pushForward(rigidBody, forwardDirection, character, deltaTime)

        if (ComponentsMapper.ambSound.has(character)) {
            ComponentsMapper.ambSound.get(character).pitchTarget = params.engineMaxPitch
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
