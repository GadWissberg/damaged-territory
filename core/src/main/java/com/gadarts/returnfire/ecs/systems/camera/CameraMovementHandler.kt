package com.gadarts.returnfire.ecs.systems.camera

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.systems.data.GameSessionData
import com.gadarts.returnfire.ecs.systems.data.SessionState
import com.gadarts.returnfire.utils.ModelUtils
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

class CameraMovementHandler(private val gameSessionData: GameSessionData) {
    private val cameraRelativeValuesMapper by lazy {
        CameraRelativeValuesMapper()
    }

    fun update(deltaTime: Float) {
        followPlayer(deltaTime)
    }

    private fun enoughTimeSinceLastCameraStateChanged(): Boolean {
        val renderData = gameSessionData.renderData
        return TimeUtils.timeSinceMillis(renderData.lastCameraStateChange) > 4000
    }

    private fun moveCameraToTargetPosition(
        deltaTime: Float
    ) {
        val player = gameSessionData.gamePlayData.player ?: return

        val renderData = gameSessionData.renderData
        val camera = renderData.camera
        val playerPosition = ModelUtils.getPositionOfModel(player)
        val finalCameraRelativeTargetPosition =
            auxVector3_1.set(renderData.cameraRelativeTargetPosition)

        val finalCameraRelativeLookAtTargetPosition =
            auxVector3_2.set(renderData.cameraRelativeTargetLookAtPosition)
        handleCameraPushWhenThrusting(
            finalCameraRelativeTargetPosition,
            finalCameraRelativeLookAtTargetPosition
        )
        renderData.cameraRelativePosition.lerp(
            finalCameraRelativeTargetPosition,
            1f - exp((-deltaTime * 0.5F))
        )
        renderData.cameraRelativeLookAtPosition.lerp(
            finalCameraRelativeLookAtTargetPosition,
            1f - exp((-deltaTime * 0.5F))
        )
        positionCamera()
        val cameraPos = camera.position
        val targetPos =
            auxVector3_1.set(playerPosition).add(renderData.cameraRelativeLookAtPosition)
        val toTarget = auxVector3_2.set(targetPos).sub(cameraPos)
        val flatDist = auxVector2_1.set(-toTarget.z, toTarget.y)
        val pitchRad = MathUtils.atan2(flatDist.y, flatDist.x)
        val pitchDeg = pitchRad * MathUtils.radDeg
        val constrainedDirection =
            auxVector3_2.set(Vector3.Z).scl(-1f).rotate(Vector3.X, pitchDeg).nor()
        camera.direction.set(constrainedDirection)
        camera.up.set(Vector3.Y)
    }

    private fun positionCamera(
    ) {
        val player = gameSessionData.gamePlayData.player ?: return

        val playerPosition = ModelUtils.getPositionOfModel(player)
        val renderData = gameSessionData.renderData
        renderData.camera.position.set(
            auxVector3_1.set(playerPosition.x, max(playerPosition.y, 0F), playerPosition.z)
                .add(renderData.cameraRelativePosition)
        )
    }

    private fun handleCameraPushWhenThrusting(
        finalCameraRelativeTargetPosition: Vector3,
        finalCameraRelativeLookAtTargetPosition: Vector3
    ) {
        val gamePlayData = gameSessionData.gamePlayData
        val player = gamePlayData.player
        if (player == null || ComponentsMapper.character.get(player).dead) return

        val thrusting = gamePlayData.playerMovementHandler.isThrusting(player)
        if (thrusting && gameSessionData.gamePlayData.sessionState != SessionState.GAME_OVER) {
            auxMatrix.idt().rotate(
                Vector3.Y,
                ComponentsMapper.modelInstance.get(player)
                    .gameModelInstance.modelInstance.transform
                    .getRotation(auxQuat)
                    .yaw
            )
            val forward = auxVector3_3.set(Vector3.X).rot(auxMatrix).nor()
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

    private fun applyRegularCameraPosition() {
        val player = gameSessionData.gamePlayData.player
        val mapping =
            cameraRelativeValuesMapper.mapping[ComponentsMapper.character.get(player).definition]
                ?: return

        gameSessionData.renderData.cameraRelativeTargetPosition.set(
            0F,
            mapping.cameraRelativeRegularY,
            mapping.cameraRelativeRegularZ,
        )
        gameSessionData.renderData.cameraRelativeTargetLookAtPosition.set(
            0F,
            0F,
            0.5F
        )
    }

    private fun updateCameraState(cameraState: CameraState) {
        val renderData = gameSessionData.renderData
        renderData.cameraState = cameraState
        renderData.lastCameraStateChange = TimeUtils.millis()
    }

    private fun followPlayer(deltaTime: Float) {
        val renderData = gameSessionData.renderData
        val player = gameSessionData.gamePlayData.player ?: return

        val mapping =
            cameraRelativeValuesMapper.mapping[ComponentsMapper.character.get(player).definition]
                ?: return

        moveCameraToTargetPosition(deltaTime)
        if (renderData.cameraState == CameraState.PLAYER_DEAD_FOCUS) {
            renderData.cameraRelativeTargetPosition.set(
                0F,
                mapping.cameraRelativeFocusY,
                0F
            )
            renderData.cameraRelativeTargetLookAtPosition.setZero()
        } else if (renderData.cameraState != CameraState.FOCUS_DEPLOYMENT) {
            val thrusting = gameSessionData.gamePlayData.playerMovementHandler.isThrusting(player)
            if (thrusting) {
                if (renderData.cameraState != CameraState.TOP) {
                    updateCameraState(CameraState.TOP)
                } else if (enoughTimeSinceLastCameraStateChanged()) {
                    renderData.cameraRelativeTargetLookAtPosition.setZero()
                    renderData.cameraRelativeTargetPosition.set(
                        0F,
                        mapping.cameraRelativeSkyY,
                        mapping.cameraRelativeSkyZ
                    )
                }
            } else if (renderData.cameraState != CameraState.REGULAR) {
                updateCameraState(CameraState.REGULAR)
            } else if (enoughTimeSinceLastCameraStateChanged()) {
                applyRegularCameraPosition()
            }
        }
    }

    fun init() {
        val player = gameSessionData.gamePlayData.player
        val mapping =
            cameraRelativeValuesMapper.mapping[ComponentsMapper.character.get(player).definition]
                ?: return

        gameSessionData.renderData.cameraRelativePosition.set(
            0F,
            mapping.cameraRelativeSkyY,
            mapping.cameraRelativeSkyZ
        )
        gameSessionData.renderData.cameraRelativeTargetPosition.set(
            0F,
            mapping.cameraRelativeFocusY,
            mapping.cameraRelativeFocusZ
        )
        positionCamera()
    }

    fun onPlayerDeployed() {
        updateCameraState(CameraState.REGULAR)
        applyRegularCameraPosition()
    }

    fun onPlayerDied() {
        updateCameraState(CameraState.PLAYER_DEAD_FOCUS)
    }

    companion object {
        private val auxVector3_1 = Vector3()
        private val auxVector3_2 = Vector3()
        private val auxVector3_3 = Vector3()
        private val auxVector2_1 = Vector2()
        private val auxQuat = com.badlogic.gdx.math.Quaternion()
        private val auxMatrix = com.badlogic.gdx.math.Matrix4()
    }
}
