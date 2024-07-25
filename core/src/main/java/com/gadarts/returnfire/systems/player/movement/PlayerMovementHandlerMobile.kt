package com.gadarts.returnfire.systems.player.movement

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.model.GameMap


class PlayerMovementHandlerMobile : PlayerMovementHandler() {
    private lateinit var camera: PerspectiveCamera
    private var desiredDirectionChanged: Boolean = false

    private fun handleRotation(player: Entity) {
        applyRotation(player)
    }

    private fun applyRotation(player: Entity) {
        val transform =
            ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance.transform
        val position = transform.getTranslation(auxVector3_1)
        val playerComponent = ComponentsMapper.player.get(player)
        val currentVelocity =
            playerComponent.getCurrentVelocity(auxVector2_1)
        transform.setToRotation(
            Vector3.Y,
            currentVelocity.angleDeg()
        )
        transform.rotate(Vector3.Z, -IDLE_Z_TILT_DEGREES)
        transform.setTranslation(position)
    }


    override fun thrust(player: Entity, directionX: Float, directionY: Float, reverse: Boolean) {
        if (directionX != 0F || directionY != 0F) {
            updateDesiredDirection()
        }
    }

    private fun updateDesiredDirection() {
        desiredDirectionChanged = true
    }

    override fun update(
        player: Entity,
        deltaTime: Float,
        currentMap: GameMap,
        dispatcher: MessageDispatcher
    ) {
        handleRotation(player)
        takeStep(
            player,
            deltaTime,
            currentMap,
            dispatcher,
            ComponentsMapper.player.get(player).getCurrentVelocity(
                auxVector2_1
            )
        )
        tiltAnimationHandler.update(player)
    }

    override fun rotate(clockwise: Int) {

    }

    override fun reverse(player: Entity) {

    }

    override fun handleAcceleration(player: Entity, maxSpeed: Float, desiredVelocity: Vector2) {
        val currentVelocity =
            ComponentsMapper.player.get(player)
                .getCurrentVelocity(auxVector2_1)
        ComponentsMapper.player.get(player).setCurrentVelocity(currentVelocity)
    }

    override fun onTouchUp(keycode: Int) {
    }


    override fun initialize(camera: PerspectiveCamera) {
        this.camera = camera
    }

    companion object {
        private val auxVector2_1 = Vector2()
        private val auxVector3_1 = Vector3()
    }
}
