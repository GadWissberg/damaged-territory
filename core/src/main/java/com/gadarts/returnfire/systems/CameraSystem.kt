package com.gadarts.returnfire.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.render.CameraState
import com.gadarts.returnfire.utils.ModelUtils
import kotlin.math.abs
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
                    if (!ComponentsMapper.player.has(msg.extraInfo as Entity)) return

                    updateCameraState(CameraState.REGULAR)
                    applyRegularCameraPosition()
                }
            })

    private fun applyRegularCameraPosition() {
        gameSessionData.renderData.cameraRelativeTargetPosition.set(
            0F,
            CAMERA_RELATIVE_REGULAR_Y,
            CAMERA_RELATIVE_REGULAR_Z
        )
        gameSessionData.renderData.cameraRelativeTargetLookAtPosition.set(
            0F,
            0F,
            0.5F
        )
    }

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
        moveCameraToTargetPosition(deltaTime)
        if (renderData.cameraState != CameraState.FOCUS_DEPLOYMENT) {
            val thrusting = gameSessionData.gamePlayData.playerMovementHandler.isThrusting(player)
            if (thrusting) {
                if (renderData.cameraState != CameraState.TOP) {
                    updateCameraState(CameraState.TOP)
                } else if (enoughTimeSinceLastCameraStateChanged()) {
                    renderData.cameraRelativeTargetLookAtPosition.setZero()
                    renderData.cameraRelativeTargetPosition.set(
                        0F,
                        CAMERA_RELATIVE_SKY_Y,
                        CAMERA_RELATIVE_SKY_Z
                    )
                }
            } else if (renderData.cameraState != CameraState.REGULAR) {
                updateCameraState(CameraState.REGULAR)
            } else if (enoughTimeSinceLastCameraStateChanged()) {
                applyRegularCameraPosition()
            }
        }
    }

    private fun enoughTimeSinceLastCameraStateChanged(): Boolean {
        val renderData = gameSessionData.renderData
        return TimeUtils.timeSinceMillis(renderData.lastCameraStateChange) > 4000
    }

    private fun updateCameraState(cameraState: CameraState) {
        val renderData = gameSessionData.renderData
        renderData.cameraState = cameraState
        renderData.lastCameraStateChange = TimeUtils.millis()
    }

    private fun moveCameraToTargetPosition(
        deltaTime: Float
    ) {
        val player = gameSessionData.gamePlayData.player ?: return

        val renderData = gameSessionData.renderData
        val camera = renderData.camera
        val playerPosition = ModelUtils.getPositionOfModel(player)
        val finalCameraRelativeTargetPosition = auxVector3_1.set(renderData.cameraRelativeTargetPosition)
        val finalCameraRelativeLookAtTargetPosition = auxVector3_2.set(renderData.cameraRelativeTargetLookAtPosition)
        handleCameraPushWhenThrusting(finalCameraRelativeTargetPosition, finalCameraRelativeLookAtTargetPosition)
        renderData.cameraRelativePosition.lerp(
            finalCameraRelativeTargetPosition,
            1f - exp((-deltaTime * 0.5F))
        )
        renderData.cameraRelativeLookAtPosition.lerp(
            finalCameraRelativeLookAtTargetPosition,
            1f - exp((-deltaTime * 0.5F))
        )
        camera.position.set(
            auxVector3_1.set(playerPosition.x, max(playerPosition.y, 0F), playerPosition.z)
                .add(renderData.cameraRelativePosition)
        )
        val cameraPos = camera.position
        val targetPos = auxVector3_1.set(playerPosition).add(renderData.cameraRelativeLookAtPosition)
        val toTarget = auxVector3_2.set(targetPos).sub(cameraPos)
        val flatDist = auxVector2_1.set(-toTarget.z, toTarget.y)
        val pitchRad = MathUtils.atan2(flatDist.y, flatDist.x)
        val pitchDeg = pitchRad * MathUtils.radDeg
        val constrainedDirection = auxVector3_2.set(Vector3.Z).scl(-1f).rotate(Vector3.X, pitchDeg).nor()
        camera.direction.set(constrainedDirection)
        camera.up.set(Vector3.Y)
    }

    private fun handleCameraPushWhenThrusting(
        finalCameraRelativeTargetPosition: Vector3,
        finalCameraRelativeLookAtTargetPosition: Vector3
    ) {
        val player = gameSessionData.gamePlayData.player ?: return

        val thrusting = gameSessionData.gamePlayData.playerMovementHandler.isThrusting(player)
        if (thrusting) {
            auxMatrix.idt().rotate(
                Vector3.Y,
                ComponentsMapper.modelInstance.get(player)
                    .gameModelInstance.modelInstance.transform
                    .getRotation(auxQuat)
                    .yaw
            )
            val forward = auxVector3_2.set(Vector3.X).rot(auxMatrix).nor()
            val absX = abs(forward.x)
            val absZ = abs(forward.z)
            val distance = if (absX > absZ) 4f else 2f
            finalCameraRelativeTargetPosition.add(
                forward.x * distance,
                0f,
                forward.z * distance
            )
            finalCameraRelativeLookAtTargetPosition.add(
                forward.x * distance,
                0f,
                forward.z * distance
            )
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
            CAMERA_RELATIVE_SKY_Y,
            playerPosition.z
        )
        camera.rotate(Vector3.X, -45F)
        gameSessionData.renderData.cameraRelativePosition.set(0F, CAMERA_RELATIVE_SKY_Y, 0F)
        gameSessionData.renderData.cameraRelativeTargetPosition.set(
            0F,
            CAMERA_RELATIVE_FOCUS_Y,
            CAMERA_RELATIVE_FOCUS_Z
        )
    }

    companion object {
        private const val CAMERA_RELATIVE_FOCUS_Y = 4F
        private const val CAMERA_RELATIVE_FOCUS_Z = 3F
        private const val CAMERA_RELATIVE_SKY_Y = 8F
        private const val CAMERA_RELATIVE_SKY_Z = 3F
        private const val CAMERA_RELATIVE_REGULAR_Y = 6F
        private const val CAMERA_RELATIVE_REGULAR_Z = 4F
        private val auxVector3_1 = Vector3()
        private val auxVector3_2 = Vector3()
        private val auxVector2_1 = Vector2()
        private val auxQuat = com.badlogic.gdx.math.Quaternion()
        private val auxMatrix = com.badlogic.gdx.math.Matrix4()
    }


}
