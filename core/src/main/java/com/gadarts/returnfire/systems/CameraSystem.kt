package com.gadarts.returnfire.systems

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CameraSystem(gamePlayManagers: GamePlayManagers) : GameEntitySystem(gamePlayManagers) {


    private var cameraTarget = Vector3()

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> =
        mapOf(SystemEvents.PLAYER_ADDED to object : HandlerOnEvent {
            override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
                initializeCamera()
            }
        })

    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        gameSessionData.renderData.camera.update()
        if (gameSessionData.gamePlayData.playerMovementHandler.isThrusting()) {
            followPlayer(2F, deltaTime)
        } else {
            followPlayer(0F, deltaTime)
        }
    }

    override fun resume(delta: Long) {
    }

    override fun dispose() {
    }


    override fun addedToEngine(engine: Engine?) {
        super.addedToEngine(engine)
    }

    private var followProgress = 0F  // Track movement progress

    private fun followPlayer(gap: Float, deltaTime: Float) {
        val player = gameSessionData.gamePlayData.player ?: return

        val transform = ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance.transform
        val playerPosition = transform.getTranslation(auxVector3_1)
        val direction = auxVector3_2.set(Vector3.X).rot(auxMatrix.set(transform.getRotation(auxQuat))).nor()
        val zOffset = Z_OFFSET * max(abs(direction.x), max(direction.z, 0F))
        val relativePosition = direction.scl(gap)

        val targetPosition = playerPosition.add(relativePosition.x, 0F, relativePosition.z + zOffset)
        val camera = gameSessionData.renderData.camera
        cameraTarget.set(targetPosition.x, camera.position.y, targetPosition.z)

        // Calculate the squared distance to avoid unnecessary sqrt operations
        val distanceSquared = camera.position.dst2(cameraTarget)

        if (distanceSquared > 0.0001F) {  // Reduced threshold for smoother final approach
            followProgress = min(followProgress + deltaTime * 0.2F, 1F)  // Increase progress smoothly

            val distance = kotlin.math.sqrt(distanceSquared)

            // Introduce a max speed limit to prevent fast snapping
            val maxSpeed = 2F  // Units per second
            val smoothFactor = Interpolation.smooth.apply(followProgress) * 0.1F
            val clampedFactor = min(smoothFactor, maxSpeed * deltaTime / distance)

            camera.position.lerp(cameraTarget, clampedFactor)
        } else {
            followProgress = max(followProgress - deltaTime * 0.5F, 0F)  // Slow decay to prevent snapping
            camera.position.lerp(cameraTarget, 0.02F)  // Small value to avoid jittering
        }
    }


    private fun initializeCamera() {
        val camera = gameSessionData.renderData.camera
        camera.update()
        val get = ComponentsMapper.modelInstance.get(gameSessionData.gamePlayData.player)
        val playerPosition = get.gameModelInstance.modelInstance.transform.getTranslation(
            auxVector3_1
        )
        camera.position.set(
            playerPosition.x,
            INITIAL_Y,
            playerPosition.z + Z_OFFSET
        )
        camera.rotate(Vector3.X, -45F)
        camera.lookAt(
            playerPosition
        )
    }

    companion object {
        private const val INITIAL_Y = 9F
        private const val Z_OFFSET = 2.5F
        private val auxVector3_1 = Vector3()
        private val auxVector3_2 = Vector3()
        private val auxQuat = Quaternion()
        private val auxMatrix = Matrix4()
    }

}
