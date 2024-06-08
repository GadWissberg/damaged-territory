package com.gadarts.returnfire.systems.player.movement

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.Vector2
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.model.GameMap
import kotlin.math.max


class PlayerMovementHandlerDesktop : PlayerMovementHandler(1F) {
    private lateinit var camera: PerspectiveCamera
    private var reverse = false

    override fun handleAcceleration(player: Entity, maxSpeed: Float) {
        val currentVelocity =
            ComponentsMapper.player.get(player)
                .getCurrentVelocity(auxVector2_1)
        if (desiredVelocity.len2() > desiredVelocitySizeThreshold) {
            currentVelocity.add(auxVector2_2.set(desiredVelocity).nor().scl(ACCELERATION))
            if (currentVelocity.len2() > maxSpeed) {
                currentVelocity.setLength2(maxSpeed)
            }
            tiltAnimationHandler.onAcceleration()
        } else {
            currentVelocity.setLength2(
                max(
                    currentVelocity.len2() - (DECELERATION),
                    1F
                )
            )
            tiltAnimationHandler.onDeceleration()
        }
        ComponentsMapper.player.get(player).setCurrentVelocity(currentVelocity)
    }

    override fun toggleStrafing(lastTouchDown: Long, player: Entity) {

    }

    override fun onTouchUp(player: Entity) {
        desiredVelocity.set(
            ComponentsMapper.player.get(player).getCurrentVelocity(auxVector2_1).nor()
        )

    }


    override fun initialize(camera: PerspectiveCamera) {
        this.camera = camera
    }

    override fun thrust(player: Entity, directionX: Float, directionY: Float, reverse: Boolean) {
        this.reverse = reverse
        desiredVelocity.scl(if (!reverse) 2F else -2F)
    }

    override fun update(
        player: Entity,
        deltaTime: Float,
        currentMap: GameMap,
        dispatcher: MessageDispatcher
    ) {
        val currentVelocity =
            ComponentsMapper.player.get(player)
                .getCurrentVelocity(auxVector2_1)
        val applyReverse = reverse && desiredVelocity.hasSameDirection(currentVelocity)
        applyRotation(player, applyReverse)
        handleAcceleration(player, if (!applyReverse) MAX_THRUST else 4F)
        takeStep(player, deltaTime, currentMap, dispatcher)
        tiltAnimationHandler.update(player)
    }

    companion object {
        private val auxVector2_1 = Vector2()
        private val auxVector2_2 = Vector2()
    }
}
