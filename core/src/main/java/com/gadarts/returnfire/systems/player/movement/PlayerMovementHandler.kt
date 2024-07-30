package com.gadarts.returnfire.systems.player.movement

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.gadarts.returnfire.model.GameMap
import com.gadarts.returnfire.systems.player.TiltAnimationHandler

abstract class PlayerMovementHandler {

    protected var tiltAnimationHandler = TiltAnimationHandler()


    protected fun rotate(rigidBody: btRigidBody, clockwise: Int) {
        val newVelocity = auxVector3_1.set(rigidBody.angularVelocity)
        if (newVelocity.len2() < MAX_ROTATION) {
            val scale = clockwise * 8F
            rigidBody.applyTorque(auxVector3_1.set(0F, 1F, 0F).scl(scale))
            auxVector3_1.set(rigidBody.angularVelocity)
            auxVector3_1.x = 0F
            auxVector3_1.z = 0F
            rigidBody.angularVelocity = auxVector3_1
        }
    }

    abstract fun onTouchUp(keycode: Int = -1)

    abstract fun initialize(camera: PerspectiveCamera)

    abstract fun thrust(
        player: Entity,
        directionX: Float = 0F,
        directionY: Float = 0F,
        reverse: Boolean = false
    )

    abstract fun update(
        player: Entity,
        deltaTime: Float,
        currentMap: GameMap,
        dispatcher: MessageDispatcher
    )

    abstract fun applyRotation(clockwise: Int)

    abstract fun reverse(player: Entity)

    companion object {
        private val auxVector3_1 = Vector3()
        private const val MAX_ROTATION = 3F
    }
}
