package com.gadarts.returnfire.systems.player.handlers.movement.apache

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper

class ApacheMovementHandlerMobile : ApacheMovementHandler() {
    private val desiredDirection = Vector2()
    private var desiredDirectionChanged: Boolean = false


    override fun thrust(character: Entity, directionX: Float, directionY: Float) {
        super.thrust(character, directionX, directionY)
        if (directionX != 0F || directionY != 0F) {
            updateDesiredDirection(directionX, directionY)
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
            pushForward(rigidBody, 1, character)
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

    override fun onMovementTouchUp(character: Entity, keycode: Int) {
        desiredDirection.setZero()
        tiltAnimationHandler.returnToRollIdle()
        tiltAnimationHandler.returnToPitchIdle()
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

    override fun pressedLeft(character: Entity) {
    }

    override fun pressedRight(character: Entity) {
    }

    override fun releasedAlt(character: Entity) {
    }


    override fun isThrusting(character: Entity): Boolean {
        return !desiredDirection.isZero
    }

    override fun isReversing(character: Entity): Boolean {
        return false
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
