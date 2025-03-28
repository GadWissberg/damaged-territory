package com.gadarts.returnfire.systems

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import kotlin.math.abs
import kotlin.math.max

class CameraSystem(gamePlayManagers: GamePlayManagers) : GameEntitySystem(gamePlayManagers) {

    private var lastZoomOut = 0L
    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> =
        mapOf(SystemEvents.PLAYER_ADDED to object : HandlerOnEvent {
            override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
                initializeCamera()
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


    override fun addedToEngine(engine: Engine?) {
        super.addedToEngine(engine)
    }

    private fun followPlayer(deltaTime: Float) {
        val player = gameSessionData.gamePlayData.player
        if (player == null || !ComponentsMapper.physics.has(player)) return

        val camera = gameSessionData.renderData.camera
        val cameraPosition = calculateCameraNextPosition(deltaTime)
        camera.position.set(cameraPosition)
    }

    private fun calculateCameraNextPosition(
        deltaTime: Float
    ): Vector3 {
        val player = gameSessionData.gamePlayData.player!!
        val playerTransform = ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance.transform
        val cameraPosition = auxVector3_4.set(gameSessionData.renderData.camera.position)
        val rotation = playerTransform.getRotation(auxQuat)
        rotation.setEulerAngles(rotation.yaw, 0F, 0F)
        val rotationVector = auxVector3_3.set(1F, 0F, 0F).rot(auxMatrix.idt().set(rotation)).nor()
        val thrusting = gameSessionData.gamePlayData.playerMovementHandler.isThrusting(player)
        val reversing = gameSessionData.gamePlayData.playerMovementHandler.isReversing(player)
        val cameraTarget = auxVector3_2.set(playerTransform.getTranslation(auxVector3_1))
            .add(
                auxVector3_5.set(rotationVector)
                    .scl(if (thrusting) CAMERA_TARGET_MOVEMENT_GAP_FORWARD else if (reversing) -CAMERA_TARGET_MOVEMENT_GAP_BACKWARDS else 0F)
                    .add(
                        0F, 0F,
                        max(abs(rotationVector.x), 0.6F) * Z_OFFSET
                    )
            )
        val movementDirection = auxVector3_3.set(cameraTarget)
            .sub(cameraPosition)
            .nor()
            .scl(deltaTime * gameSessionData.fpsTarget * (if (thrusting) 0.07F else 0.05F))
        movementDirection.y = 0F
        cameraPosition.y = 0F
        cameraTarget.y = 0F
        val dst2 = cameraPosition.dst2(cameraTarget)
        if (dst2 > 0.1F) {
            cameraPosition.add(movementDirection)
        } else if (dst2 > 0.005F) {
            cameraPosition.slerp(cameraTarget, 0.05F)
        }
        cameraPosition.y = gameSessionData.renderData.camera.position.y
        handleCameraZoom(cameraPosition)
        return cameraPosition
    }

    private fun handleCameraZoom(cameraPosition: Vector3) {
        val player = gameSessionData.gamePlayData.player
        val playerVelocity = ComponentsMapper.physics.get(player).rigidBody.linearVelocity
        if (playerVelocity.len2() > 0.1F && cameraPosition.y < MAX_Y) {
            cameraPosition.y = Interpolation.sine.apply(cameraPosition.y, MAX_Y, ZOOM_PACE)
            lastZoomOut = TimeUtils.millis()
        } else if (cameraPosition.y > MIN_Y && TimeUtils.timeSinceMillis(lastZoomOut) > 4000L) {
            cameraPosition.y = Interpolation.sine.apply(cameraPosition.y, MIN_Y, ZOOM_PACE)
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
            MIN_Y,
            playerPosition.z + Z_OFFSET
        )
        camera.rotate(Vector3.X, -45F)
        camera.lookAt(
            playerPosition
        )
    }

    companion object {
        private const val ZOOM_PACE: Float = 0.02F
        private const val MAX_Y = 12F
        private const val MIN_Y = 9F
        private const val Z_OFFSET = 1.5F
        private const val CAMERA_TARGET_MOVEMENT_GAP_FORWARD = 5.5F
        private const val CAMERA_TARGET_MOVEMENT_GAP_BACKWARDS = CAMERA_TARGET_MOVEMENT_GAP_FORWARD / 2F
        private val auxVector3_1 = Vector3()
        private val auxVector3_2 = Vector3()
        private val auxVector3_3 = Vector3()
        private val auxVector3_4 = Vector3()
        private val auxVector3_5 = Vector3()
        private val auxMatrix = Matrix4()
        private val auxQuat = Quaternion()
    }

}
