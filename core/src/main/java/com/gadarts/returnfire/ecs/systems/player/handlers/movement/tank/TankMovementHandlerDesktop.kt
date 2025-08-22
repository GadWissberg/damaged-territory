package com.gadarts.returnfire.ecs.systems.player.handlers.movement.tank

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.systems.player.handlers.movement.GroundVehicleMovementHandler

class TankMovementHandlerDesktop :
    GroundVehicleMovementHandler(TankMovementHandlerParams()) {
    private var turretRotationEnabled: Boolean = false
    private var movement: Int = 0
    private var rotation: Int = 0

    override fun idleEngineSound(character: Entity) {
        super.idleEngineSound(character)
        stopMovement()
    }

    override fun stopMovement() {
        super.stopMovement()
        movement = 0
    }

    override fun onMovementTouchUp(character: Entity, keycode: Int) {
        when (keycode) {
            Input.Keys.UP -> {
                stopFunctionForVerticalKey(MOVEMENT_FORWARD, character)
            }

            Input.Keys.DOWN -> {
                stopFunctionForVerticalKey(MOVEMENT_REVERSE, character)
            }

            Input.Keys.LEFT -> {
                stopFunctionForSideKey(rotation > 0F, character)
            }

            Input.Keys.RIGHT -> {
                stopFunctionForSideKey(rotation < 0F, character)
            }

        }
    }

    private fun stopFunctionForVerticalKey(movementConstant: Int, character: Entity) {
        if (movement == movementConstant) {
            idleEngineSound(character)
        }
    }

    private fun stopFunctionForSideKey(rotationCheck: Boolean, character: Entity) {
        if (turretRotationEnabled) {
            val turretComponent = ComponentsMapper.turret.get(ComponentsMapper.turretBase.get(character).turret)
            turretComponent.turretRotating = 0
        } else if (rotationCheck) {
            rotation = 0
            val rigidBody = ComponentsMapper.physics.get(character).rigidBody
            rigidBody.angularFactor = Vector3.Zero
        }
    }


    override fun thrust(character: Entity, directionX: Float, directionY: Float) {
        movement = MOVEMENT_FORWARD
    }

    override fun update(character: Entity, deltaTime: Float) {
        super.update(character, deltaTime)
        val rigidBody = ComponentsMapper.physics.get(character).rigidBody
        if (movement != 0) {
            pushForward(rigidBody, movement, character, deltaTime)
        }
        if (rotation != ROTATION_IDLE) {
            rotate(rigidBody, rotation)
        }
    }

    override fun onTurretTouchPadTouchDown(deltaX: Float, deltaY: Float, character: Entity) {

    }

    override fun onTurretTouchPadTouchUp(character: Entity) {

    }

    override fun strafe(left: Boolean) {
    }

    override fun isStrafing(): Boolean {
        return false
    }

    override fun stopStrafe() {
    }

    override fun pressedAlt(character: Entity) {
        turretRotationEnabled = true
        if (rotation != 0) {
            val turretComponent = ComponentsMapper.turret.get(ComponentsMapper.turretBase.get(character).turret)
            turretComponent.turretRotating = rotation
            applyRotation(0, character)
        }
    }

    override fun pressedLeft(character: Entity) {
        if (turretRotationEnabled) {
            applyTurretRotation(1, ComponentsMapper.turretBase.get(character).turret)
        } else {
            applyRotation(1, character)
        }
    }

    override fun pressedRight(character: Entity) {
        if (turretRotationEnabled) {
            applyTurretRotation(-1, ComponentsMapper.turretBase.get(character).turret)
        } else {
            applyRotation(-1, character)
        }
    }

    fun applyTurretRotation(side: Int, turret: Entity) {
        ComponentsMapper.turret.get(turret).turretRotating = side
    }

    override fun releasedAlt(character: Entity) {
        turretRotationEnabled = false
        val turretComponent = ComponentsMapper.turret.get(ComponentsMapper.turretBase.get(character).turret)
        val turretRotating = turretComponent.turretRotating
        if (turretRotating != 0) {
            applyRotation(turretRotating, character)
        }
        turretComponent.turretRotating = 0
    }

    override fun isThrusting(character: Entity): Boolean {
        return movement > 0
    }

    override fun isReversing(character: Entity): Boolean {
        return movement < 0
    }

    override fun applyRotation(clockwise: Int, character: Entity) {
        super.applyRotation(clockwise, character)
        rotation = clockwise
    }

    override fun reverse() {
        movement = MOVEMENT_REVERSE
    }

    companion object {
        private const val MOVEMENT_FORWARD = 1
        private const val MOVEMENT_REVERSE = -1
        private const val ROTATION_IDLE = 0
    }
}
