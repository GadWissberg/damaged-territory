package com.gadarts.returnfire.systems

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.ai.msg.Telegram
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


    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> =
        mapOf(SystemEvents.PLAYER_ADDED to object : HandlerOnEvent {
            override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
                initializeCamera()
            }
        })

    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        gameSessionData.renderData.camera.update()
        followPlayer(deltaTime)
    }

    override fun resume(delta: Long) {
    }

    override fun dispose() {
    }


    override fun addedToEngine(engine: Engine?) {
        super.addedToEngine(engine)
    }

    private fun followPlayer(deltaTime: Float) {
        val player = gameSessionData.gamePlayData.player
        if (player == null || !ComponentsMapper.physics.has(player)) return

        val playerTransform = ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance.transform
        val playerPosition = playerTransform.getTranslation(auxVector3_1)
        val camera = gameSessionData.renderData.camera
        val cameraPosition = auxVector3_4.set(camera.position)
        val rotation = playerTransform.getRotation(auxQuat)
        rotation.setEulerAngles(rotation.yaw, 0F, 0F)
        val rotationVector = auxVector3_3.set(1F, 0F, 0F).rot(auxMatrix.idt().set(rotation)).nor()
        val cameraTarget = auxVector3_2.set(playerPosition)
            .add(auxVector3_5.set(rotationVector).scl(2F)).add(
                0F, 0F,
                max(abs(rotationVector.x), if (rotationVector.z >= 0) 0.8F else 0.5F) * Z_OFFSET
            )
        val movementDirection =
            auxVector3_3.set(cameraTarget).sub(cameraPosition).nor()
                .scl(deltaTime * gameSessionData.fpsTarget * 2F * 0.1F)

        movementDirection.y = 0F
        cameraPosition.y = 0F
        cameraTarget.y = 0F
        val dst2 = cameraPosition.dst2(cameraTarget)
        if (dst2 > 0.1F) {
            cameraPosition.add(movementDirection)
        } else if (dst2 > 0.005F) {
            cameraPosition.slerp(cameraTarget, 0.01F)
        }
        cameraPosition.y = camera.position.y
        camera.position.set(cameraPosition)
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
        private val auxVector3_3 = Vector3()
        private val auxVector3_4 = Vector3()
        private val auxVector3_5 = Vector3()
        private val auxMatrix = Matrix4()
        private val auxQuat = Quaternion()
    }

}
