package com.gadarts.returnfire.systems

import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.utils.ModelUtils
import kotlin.math.exp
import kotlin.math.max

class CameraSystem(gamePlayManagers: GamePlayManagers) : GameEntitySystem(gamePlayManagers) {

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> =
        mapOf(SystemEvents.PLAYER_ADDED to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                gamePlayManagers: GamePlayManagers
            ) {
                initializeCamera()
            }
        },
            SystemEvents.CHARACTER_DEPLOYED to object : HandlerOnEvent {
                override fun react(
                    msg: Telegram,
                    gameSessionData: GameSessionData,
                    gamePlayManagers: GamePlayManagers
                ) {
                    gameSessionData.renderData.cameraRelativeTargetPosition.set(
                        0F,
                        CAMERA_RELATIVE_Y_MED,
                        CAMERA_RELATIVE_Z_MED
                    )
                    gameSessionData.renderData.cameraRelativeTargetLookAtPosition.set(
                        0F,
                        0F,
                        1F
                    )
                }
            })

    override fun update(deltaTime: Float) {
        gameSessionData.renderData.camera.update()
        followPlayer(deltaTime)
    }

    override fun resume(delta: Long) {
    }

    override fun dispose() {
    }


    private fun followPlayer(deltaTime: Float) {
        val player = gameSessionData.gamePlayData.player ?: return

        val renderData = gameSessionData.renderData
        val camera = renderData.camera
        val playerPosition = ModelUtils.getPositionOfModel(player)
        renderData.cameraRelativePosition.lerp(
            renderData.cameraRelativeTargetPosition,
            1f - exp((-deltaTime * 0.5F))
        )
        renderData.cameraRelativeLookAtPosition.lerp(
            renderData.cameraRelativeTargetLookAtPosition,
            1f - exp((-deltaTime * 0.5F))
        )
        camera.position.set(
            auxVector3_1.set(playerPosition.x, max(playerPosition.y, 0F), playerPosition.z)
                .add(renderData.cameraRelativePosition)
        )
        camera.lookAt(
            auxVector3_1.set(playerPosition).add(
                renderData.cameraRelativeLookAtPosition
            )
        )
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
            CAMERA_RELATIVE_Y_HIGH,
            playerPosition.z
        )
        camera.rotate(Vector3.X, -45F)
        gameSessionData.renderData.cameraRelativePosition.set(0F, CAMERA_RELATIVE_Y_HIGH, 0F)
        gameSessionData.renderData.cameraRelativeTargetPosition.set(
            0F,
            CAMERA_RELATIVE_Y_CLOSE,
            CAMERA_RELATIVE_Z_CLOSE
        )
    }

    companion object {
        private const val CAMERA_RELATIVE_Y_CLOSE = 3F
        private const val CAMERA_RELATIVE_Z_CLOSE = 2F
        private const val CAMERA_RELATIVE_Y_HIGH = 14F
        private const val CAMERA_RELATIVE_Y_MED = 7F
        private const val CAMERA_RELATIVE_Z_MED = 3F
        private val auxVector3_1 = Vector3()
    }


}
