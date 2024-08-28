package com.gadarts.returnfire.systems

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents

class CameraSystem : GameEntitySystem() {


    private var cameraTarget = Vector3()

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = emptyMap()

    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        gameSessionData.renderData.camera.update()
        followPlayer()
    }

    override fun resume(delta: Long) {
    }

    override fun dispose() {
    }

    override fun initialize(gameSessionData: GameSessionData, managers: Managers) {
        super.initialize(gameSessionData, managers)
        initializeCamera()
    }

    override fun addedToEngine(engine: Engine?) {
        super.addedToEngine(engine)
    }

    private fun followPlayer() {
        val player = gameSessionData.player
        val transform =
            ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance.transform
        val playerPosition = transform.getTranslation(auxVector3_1)
        followPlayerRegularMovement(playerPosition)
    }

    private fun followPlayerRegularMovement(
        playerPosition: Vector3
    ) {
        auxVector2.set(2F, 0F).setAngleDeg(
            ComponentsMapper.modelInstance.get(gameSessionData.player).gameModelInstance.modelInstance.transform.getRotation(
                auxQuat
            ).yaw
        )
        cameraTarget =
            playerPosition.add(auxVector2.x, 0F, -auxVector2.y + Z_OFFSET)
        val camera = gameSessionData.renderData.camera
        cameraTarget.y = camera.position.y
        camera.position.interpolate(cameraTarget, 0.2F, Interpolation.exp5)
    }


    private fun initializeCamera() {
        val camera = gameSessionData.renderData.camera
        camera.near = NEAR
        camera.far = FAR
        camera.update()
        val get = ComponentsMapper.modelInstance.get(gameSessionData.player)
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
        private const val NEAR = 0.1F
        private const val FAR = 300F
        private const val INITIAL_Y = 8F
        private const val Z_OFFSET = 2F
        private val auxVector2 = Vector2()
        private val auxVector3_1 = Vector3()
        private val auxQuat = Quaternion()
    }

}
