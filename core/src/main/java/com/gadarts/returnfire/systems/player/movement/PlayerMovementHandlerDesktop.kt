package com.gadarts.returnfire.systems.player.movement

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Input
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.model.GameMap


class PlayerMovementHandlerDesktop : PlayerMovementHandler() {
    private var movement: Int = 0
    private var rotation: Int = 0
    private lateinit var camera: PerspectiveCamera

    override fun onTouchUp(keycode: Int) {
        when (keycode) {
            Input.Keys.UP -> {
                if (movement == MOVEMENT_FORWARD) {
                    movement = MOVEMENT_IDLE
                    tiltAnimationHandler.returnToRollIdle()
                }
            }

            Input.Keys.DOWN -> {
                if (movement == MOVEMENT_REVERSE) {
                    movement = MOVEMENT_IDLE
                    tiltAnimationHandler.returnToRollIdle()
                }
            }

            Input.Keys.LEFT -> {
                if (rotation > 0F) {
                    rotation = 0
                    tiltAnimationHandler.returnToPitchIdle()
                }
            }

            Input.Keys.RIGHT -> {
                if (rotation < 0F) {
                    rotation = 0
                    tiltAnimationHandler.returnToPitchIdle()
                }
            }

        }
    }


    override fun initialize(camera: PerspectiveCamera) {
        this.camera = camera
    }

    override fun thrust(player: Entity, directionX: Float, directionY: Float, reverse: Boolean) {
        movement = MOVEMENT_FORWARD
        tiltAnimationHandler.tiltForward()
    }

    override fun update(
        player: Entity,
        deltaTime: Float,
        currentMap: GameMap,
        dispatcher: MessageDispatcher
    ) {
        val rigidBody = ComponentsMapper.physics.get(player).rigidBody
        if (movement != MOVEMENT_IDLE) {
            val newVelocity = auxVector3_1.set(rigidBody.linearVelocity)
            if (newVelocity.len2() < MAX_VELOCITY) {
                val forward = rigidBody.worldTransform.getRotation(auxQuaternion1)
                    .transform(auxVector3_2.set(movement * 1F, 0F, 0F))
                rigidBody.applyCentralForce(
                    auxVector3_1.set(forward.x, 0F, forward.z).scl(if (movement > MOVEMENT_IDLE) 50F else 25F)
                )
            }
        }
        if (rotation != ROTATION_IDLE) {
            rotate(rigidBody, rotation)
        }
        val velocity: Vector3 = rigidBody.linearVelocity
        val lateralVelocity = auxVector3_1.set(velocity.x, 0f, velocity.z)
        val dampingForce: Vector3 = lateralVelocity.scl(-15F)
        rigidBody.applyCentralForce(dampingForce)
        syncModelInstanceTransformToRigidBody(player, rigidBody)
        tiltAnimationHandler.update(player)
    }


    private fun syncModelInstanceTransformToRigidBody(
        player: Entity,
        rigidBody: btRigidBody
    ) {
        val modelInstanceComponent = ComponentsMapper.modelInstance.get(player)
        modelInstanceComponent.gameModelInstance.modelInstance.transform.getRotation(auxQuaternion1)
        auxQuaternion1.setEulerAngles(0F, auxQuaternion1.pitch, auxQuaternion1.roll)
        modelInstanceComponent.gameModelInstance.modelInstance.transform.setToTranslation(
            rigidBody.worldTransform.getTranslation(
                auxVector3_1
            ),
        )
        auxQuaternion2.idt().setEulerAngles(
            rigidBody.worldTransform.getRotation(auxQuaternion3).yaw,
            auxQuaternion1.pitch,
            auxQuaternion1.roll
        )
        modelInstanceComponent.gameModelInstance.modelInstance.transform.rotate(auxQuaternion2)
    }


    override fun applyRotation(clockwise: Int) {
        rotation = clockwise
        tiltAnimationHandler.lateralTilt(clockwise)
    }

    override fun reverse(player: Entity) {
        movement = MOVEMENT_REVERSE
        tiltAnimationHandler.tiltBackwards()
    }

    companion object {
        private val auxVector3_1 = Vector3()
        private val auxVector3_2 = Vector3()
        private const val MAX_VELOCITY = 6F
        private const val MOVEMENT_FORWARD = 1
        private const val MOVEMENT_IDLE = 0
        private const val MOVEMENT_REVERSE = -1
        private const val ROTATION_IDLE = 0
        private val auxQuaternion1 = Quaternion()
        private val auxQuaternion2 = Quaternion()
        private val auxQuaternion3 = Quaternion()
    }
}
