package com.gadarts.returnfire.systems.player.handlers.movement.jeep

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.FrontWheelsComponent
import com.gadarts.returnfire.components.physics.RigidBody
import com.gadarts.returnfire.systems.player.handlers.movement.GroundVehicleMovementHandler
import kotlin.math.max
import kotlin.math.min

class JeepMovementHandlerDesktop : GroundVehicleMovementHandler(
    2.5F,
    30F,
    15F,
    5F,
    2F
) {
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

    private fun stopFunctionForVerticalKey(movementConstant: Int, character: Entity) {
        if (movement == movementConstant) {
            idleEngineSound(character)
        }
    }

    override fun thrust(character: Entity, directionX: Float, directionY: Float) {
        movement = MOVEMENT_FORWARD
    }

    override fun pressedLeft(character: Entity) {
        rotateFrontWheels(character, 1)
    }

    private fun rotateFrontWheels(character: Entity, side: Int) {
        val frontWheelsComponent = ComponentsMapper.frontWheelsComponent.get(character)
        frontWheelsComponent.steeringSide = side
    }

    override fun pressedRight(character: Entity) {
        rotateFrontWheels(character, -1)
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

    override fun update(character: Entity, deltaTime: Float) {
        super.update(character, deltaTime)
        val rigidBody = ComponentsMapper.physics.get(character).rigidBody
        if (movement != 0) {
            pushForward(rigidBody, movement, character, deltaTime)
        }
        if (rotation != ROTATION_IDLE) {
            rotate(rigidBody, rotation)
        }
        val frontWheelsComponent = ComponentsMapper.frontWheelsComponent.get(character)
        val steeringRotation = frontWheelsComponent.steeringRotation
        if (frontWheelsComponent.steeringSide > 0) {
            frontWheelsComponent.steeringRotation = min(steeringRotation + 1F, STEERING_MAX_ANGLE)
        } else if (frontWheelsComponent.steeringSide < 0) {
            frontWheelsComponent.steeringRotation = max(steeringRotation - 1F, -STEERING_MAX_ANGLE)
        } else if (steeringRotation > 0) {
            frontWheelsComponent.steeringRotation = max(steeringRotation - 1F, 0F)
        } else if (steeringRotation < 0) {
            frontWheelsComponent.steeringRotation = min(steeringRotation + 1F, 0F)
        }
        handleRotation(rigidBody, frontWheelsComponent, character)
    }

    private fun handleRotation(
        rigidBody: RigidBody,
        frontWheelsComponent: FrontWheelsComponent,
        character: Entity
    ) {
        val moving = isMoving(rigidBody)
        if (frontWheelsComponent.steeringRotation != 0F && moving != 0) {
            val clockwise = (if (frontWheelsComponent.steeringRotation > 0F) 1 else -1) * (if (moving > 0) 1 else -1)
            applyRotation(clockwise, character)
        } else {
            rotation = 0
        }
    }

    private fun isMoving(rigidBody: RigidBody): Int {
        val worldVelocity = auxVector3_1.set(rigidBody.linearVelocity)
        val worldRotation = rigidBody.worldTransform.getRotation(auxQuaternion1)
        val inverseRotation = auxQuaternion2.set(worldRotation).conjugate()
        inverseRotation.transform(worldVelocity)
        return if (worldVelocity.x > 0.1f) 1 else if (worldVelocity.x < -0.1f) -1 else 0
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

    private fun stopFunctionForSideKey(rotationCheck: Boolean, character: Entity) {
        if (rotationCheck) {
            rotation = 0
            val rigidBody = ComponentsMapper.physics.get(character).rigidBody
            rigidBody.angularFactor = Vector3.Zero
        }
        ComponentsMapper.frontWheelsComponent.get(character).steeringSide = 0
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
    }


    override fun releasedAlt(character: Entity) {
    }


    companion object {
        private const val MOVEMENT_FORWARD = 1
        private const val MOVEMENT_REVERSE = -1
        private const val ROTATION_IDLE = 0
        private const val STEERING_MAX_ANGLE = 30F
        private val auxVector3_1 = Vector3()
        private val auxQuaternion1 = com.badlogic.gdx.math.Quaternion()
        private val auxQuaternion2 = com.badlogic.gdx.math.Quaternion()
    }
}
