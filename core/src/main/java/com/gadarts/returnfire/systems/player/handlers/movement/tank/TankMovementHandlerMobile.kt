package com.gadarts.returnfire.systems.player.handlers.movement.tank

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.physics.RigidBody

class TankMovementHandlerMobile(rigidBody: RigidBody, player: Entity) :
    TankMovementHandler(rigidBody, player) {
    private val desiredDirection = Vector2()
    private lateinit var camera: PerspectiveCamera
    private var desiredDirectionChanged: Boolean = false


    override fun thrust(player: Entity, directionX: Float, directionY: Float) {
        if (directionX != 0F || directionY != 0F) {
            updateDesiredDirection(directionX, directionY)
        }
    }

    override fun update(
        player: Entity,
        deltaTime: Float,
    ) {
        super.update(player, deltaTime)
        val rigidBody = ComponentsMapper.physics.get(player).rigidBody
        if (!desiredDirection.isZero) {
            rigidBody.worldTransform.getRotation(auxQuaternion)
                .transform(auxVector3.set(1F, 0F, 0F))
            pushForward(rigidBody, 1)
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

    override fun reverse() {

    }

    override fun onMovementTouchPadTouchUp(keycode: Int) {
        desiredDirection.setZero()
        idleEngineSound()
    }

    override fun onTurretTouchPadTouchDown(deltaX: Float, deltaY: Float) {
        val turret = ComponentsMapper.turretBase.get(player).turret
        val turretInstance =
            ComponentsMapper.modelInstance.get(turret).gameModelInstance.modelInstance
        val currentYaw = turretInstance.transform.getRotation(auxQuaternion).yaw
        val targetAngle = auxVector2.set(deltaX, deltaY).angleDeg()
        var angleDifference = targetAngle - currentYaw
        angleDifference = (angleDifference + 360F) % 360F
        turretRotating = when {
            angleDifference < ROTATION_EPSILON || angleDifference > 360 - ROTATION_EPSILON -> 0
            angleDifference > 180 -> -1
            else -> 1
        }
    }

    override fun onTurretTouchPadTouchUp() {
        turretRotating = 0
    }


    override fun initialize(camera: PerspectiveCamera) {
        this.camera = camera
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
