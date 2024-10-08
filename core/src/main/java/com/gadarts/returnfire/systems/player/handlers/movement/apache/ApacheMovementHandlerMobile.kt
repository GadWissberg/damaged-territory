package com.gadarts.returnfire.systems.player.handlers.movement.apache

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper

class ApacheMovementHandlerMobile : ApacheMovementHandler() {
    private val desiredDirection = Vector2()
    private lateinit var camera: PerspectiveCamera
    private var desiredDirectionChanged: Boolean = false


    override fun thrust(player: Entity, directionX: Float, directionY: Float) {
        super.thrust(player, directionX, directionY)
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
            if (!MathUtils.isEqual(
                    auxQuaternion.yaw + (if (auxQuaternion.yaw >= 0) 0F else 360F),
                    desiredDirection.angleDeg(),
                    1F
                )
            ) {
                val diff = desiredDirection.angleDeg() - auxQuaternion.yaw
                val negativeRotation = auxVector2.set(1F, 0F).setAngleDeg(diff).angleDeg() > 180
                val clockwise = if (negativeRotation) -1 else 1
                rotate(rigidBody, clockwise)
                tiltAnimationHandler.lateralTilt(clockwise)
            } else {
                rigidBody.angularVelocity = auxVector3.setZero()
            }
        }
    }

    override fun reverse() {

    }

    override fun onMovementTouchPadTouchUp(keycode: Int) {
        desiredDirection.setZero()
        tiltAnimationHandler.returnToRollIdle()
        tiltAnimationHandler.returnToPitchIdle()
    }

    override fun onTurretTouchPadTouchDown(deltaX: Float, deltaY: Float) {

    }

    override fun onTurretTouchPadTouchUp() {

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
    }
}
