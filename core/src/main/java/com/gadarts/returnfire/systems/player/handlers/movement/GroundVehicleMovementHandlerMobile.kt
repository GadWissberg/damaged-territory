package com.gadarts.returnfire.systems.player.handlers.movement

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper

class GroundVehicleMovementHandlerMobile(params: GroundVehicleMovementHandlerParams) :
    GroundVehicleMovementHandler(params) {
    private val desiredDirection = Vector2()
    private var desiredDirectionChanged: Boolean = false
    private var movement: Int = 0


    override fun thrust(character: Entity, directionX: Float, directionY: Float) {
        if (directionX != 0F || directionY != 0F) {
            updateDesiredDirection(directionX, directionY)
            movement = 1
        }
    }

    override fun update(
        character: Entity,
        deltaTime: Float,
    ) {
        super.update(character, deltaTime)
        val rigidBody = ComponentsMapper.physics.get(character).rigidBody
        if (!desiredDirection.isZero) {
            rigidBody.worldTransform.getRotation(auxQuaternion)
                .transform(auxVector3.set(1F, 0F, 0F))
            pushForward(rigidBody, if (movement >= 0) 1 else -1, character, deltaTime)
            val yaw = auxQuaternion.yaw
            if (!MathUtils.isEqual(
                    yaw + (if (yaw >= 0) 0F else 360F),
                    desiredDirection.angleDeg(),
                    1F
                )
            ) {
                val diff = desiredDirection.angleDeg() - yaw
                val negativeRotation = auxVector2.set(1F, 0F).setAngleDeg(diff).angleDeg() > 180
                val clockwise = if (negativeRotation) -1 else 1
                rotate(rigidBody, clockwise)
                rigidBody.angularFactor = Vector3.Y
            } else {
                rigidBody.angularVelocity = auxVector3.setZero()
                rigidBody.angularFactor = Vector3.Zero
            }
        }
    }

    override fun onReverseScreenButtonReleased(character: Entity) {
        stopMoving(character)
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

    override fun pressedLeft(character: Entity) {
    }

    override fun pressedRight(character: Entity) {
    }

    override fun releasedAlt(character: Entity) {
    }

    override fun isThrusting(character: Entity): Boolean {
        return movement == 1
    }

    override fun isReversing(character: Entity): Boolean {
        return movement == -1
    }

    override fun onReverseScreenButtonPressed(character: Entity) {
        val direction = auxVector2.set(Vector2.X).setAngleDeg(
            ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform.getRotation(
                auxQuaternion
            ).yaw
        )
        updateDesiredDirection(
            direction.x, direction.y
        )
        movement = -1
    }

    override fun reverse() {

    }

    override fun onMovementTouchUp(character: Entity, keycode: Int) {
        stopMoving(character)
    }

    private fun stopMoving(character: Entity) {
        desiredDirection.setZero()
        idleEngineSound(character)
    }

    override fun onTurretTouchPadTouchDown(deltaX: Float, deltaY: Float, character: Entity) {
        val turret = ComponentsMapper.turretBase.get(character).turret
        val turretInstance =
            ComponentsMapper.modelInstance.get(turret).gameModelInstance.modelInstance
        val currentYaw = turretInstance.transform.getRotation(auxQuaternion).yaw
        val targetAngle = auxVector2.set(deltaX, deltaY).angleDeg()
        var angleDifference = targetAngle - currentYaw
        angleDifference = (angleDifference + 360F) % 360F
        ComponentsMapper.turret.get(turret).turretRotating = when {
            angleDifference < ROTATION_EPSILON || angleDifference > 360 - ROTATION_EPSILON -> 0
            angleDifference > 180 -> -1
            else -> 1
        }
    }

    override fun onTurretTouchPadTouchUp(character: Entity) {
        ComponentsMapper.turret.get(ComponentsMapper.turretBase.get(character).turret).turretRotating =
            0
    }


    private fun updateDesiredDirection(directionX: Float, directionY: Float) {
        desiredDirectionChanged = true
        desiredDirection.set(directionX, directionY)
    }

    companion object {
        private val auxVector3 = Vector3()
        private val auxVector2 = Vector2()
        private val auxQuaternion = Quaternion()
        private const val ROTATION_EPSILON = 2F
    }
}