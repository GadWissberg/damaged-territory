package com.gadarts.returnfire.systems.player.handlers.movement.tank

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.systems.player.handlers.movement.VehicleMovementHandler

abstract class TankMovementHandler :
    VehicleMovementHandler(
        -30F,
        4F,
        45F,
        25F,
        6F
    ) {
    protected var turretRotating: Int = 0

    protected open fun idleEngineSound(character: Entity) {
        ComponentsMapper.ambSound.get(character).pitchTarget = 1F
    }

    override fun applyRotation(clockwise: Int, character: Entity) {
        val rigidBody = ComponentsMapper.physics.get(character).rigidBody
        rigidBody.angularFactor = Vector3.Y
    }

    override fun pushForward(rigidBody: btRigidBody, forwardDirection: Int, character: Entity) {
        super.pushForward(rigidBody, forwardDirection, character)
        ComponentsMapper.ambSound.get(character).pitchTarget = 2F
    }

    override fun update(character: Entity, deltaTime: Float) {
        super.update(character, deltaTime)
        if (turretRotating != 0) {
            val turretRelativeRotation =
                ComponentsMapper.turret.get(ComponentsMapper.turretBase.get(character).turret).turretRelativeRotation
            ComponentsMapper.turret.get(ComponentsMapper.turretBase.get(character).turret).turretRelativeRotation =
                (turretRelativeRotation + turretRotating * 0.75F * deltaTime * 60F) % 360F
        }
    }
}
