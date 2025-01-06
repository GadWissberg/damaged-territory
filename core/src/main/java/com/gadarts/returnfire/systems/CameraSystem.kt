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
            followPlayer(4F)
        } else {
            followPlayer(0F)
        }
    }

    override fun resume(delta: Long) {
    }

    override fun dispose() {
    }


    override fun addedToEngine(engine: Engine?) {
        super.addedToEngine(engine)
    }

    private fun followPlayer(gap: Float) {
        val player = gameSessionData.gamePlayData.player ?: return

        val transform =
            ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance.transform
        val playerPosition = transform.getTranslation(auxVector3_1)
        val direction = auxVector3_2.set(Vector3.X).rot(
            auxMatrix.set(
                transform.getRotation(
                    auxQuat
                )
            )
        ).nor()
        val zOffset = Z_OFFSET * max(abs(direction.x), 0F)
        val relativePosition =
            direction.scl(gap)
        cameraTarget =
            playerPosition.add(relativePosition.x, 0F, relativePosition.z + zOffset)

        val camera = gameSessionData.renderData.camera
        cameraTarget.y = camera.position.y
        camera.position.interpolate(cameraTarget, 0.01F, Interpolation.linear)
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
