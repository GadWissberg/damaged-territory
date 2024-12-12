package com.gadarts.returnfire.systems

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents

class CameraSystem(managers: Managers) : GameEntitySystem(managers) {


    private var cameraTarget = Vector3()

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = emptyMap()

    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        gameSessionData.renderData.camera.update()
        followPlayer(deltaTime)
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

    private fun followPlayer(deltaTime: Float) {
        val player = gameSessionData.gameplayData.player
        val transform =
            ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance.transform
        val playerPosition = transform.getTranslation(auxVector3_1)
        followPlayerRegularMovement(playerPosition, deltaTime)
    }

    private fun followPlayerRegularMovement(
        playerPosition: Vector3,
        deltaTime: Float
    ) {
        val physicsComponent = ComponentsMapper.physics.get(gameSessionData.gameplayData.player) ?: return

        val linearVelocity =
            auxVector3_2.set(physicsComponent.rigidBody.linearVelocity).scl(gameSessionData.fpsTarget * deltaTime * 4F)
        cameraTarget =
            playerPosition.add(linearVelocity.x, 0F, linearVelocity.z + Z_OFFSET)
        val camera = gameSessionData.renderData.camera
        cameraTarget.y = camera.position.y
        camera.position.interpolate(cameraTarget, 0.025F, Interpolation.exp5)
    }


    private fun initializeCamera() {
        val camera = gameSessionData.renderData.camera
        camera.update()
        val get = ComponentsMapper.modelInstance.get(gameSessionData.gameplayData.player)
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
        private const val Z_OFFSET = 3F
        private val auxVector3_1 = Vector3()
        private val auxVector3_2 = Vector3()
    }

}
