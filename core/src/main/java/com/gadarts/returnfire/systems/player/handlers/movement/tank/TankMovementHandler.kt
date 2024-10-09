package com.gadarts.returnfire.systems.player.handlers.movement.tank

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.physics.RigidBody
import com.gadarts.returnfire.systems.player.handlers.movement.VehicleMovementHandler

abstract class TankMovementHandler(private val rigidBody: RigidBody, protected val player: Entity) :
    VehicleMovementHandler(
        -30F,
        4F,
        45F,
        25F,
        6F
    ) {
    protected var turretRotating: Int = 0

    protected open fun idleEngineSound() {
        ComponentsMapper.ambSound.get(player).pitchTarget = 1F
    }

    override fun applyRotation(clockwise: Int) {
        rigidBody.angularFactor = Vector3.Y
    }

    override fun pushForward(rigidBody: btRigidBody, forwardDirection: Int) {
        super.pushForward(rigidBody, forwardDirection)
        ComponentsMapper.ambSound.get(player).pitchTarget = 2F
    }

    override fun update(player: Entity, deltaTime: Float) {
        super.update(player, deltaTime)
        if (turretRotating != 0) {
            val turretRelativeRotation =
                ComponentsMapper.turret.get(ComponentsMapper.turretBase.get(player).turret).turretRelativeRotation
            ComponentsMapper.turret.get(ComponentsMapper.turretBase.get(player).turret).turretRelativeRotation =
                (turretRelativeRotation + turretRotating * 0.75F * deltaTime * 60F) % 360F
        }
    }
}
