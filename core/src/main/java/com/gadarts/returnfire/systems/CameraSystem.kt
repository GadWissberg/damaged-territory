package com.gadarts.returnfire.systems

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents

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
        val player = gameSessionData.gamePlayData.player ?: return

        val transform =
            ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance.transform
        val playerPosition = transform.getTranslation(auxVector3_1)
        followPlayerRegularMovement(playerPosition, deltaTime)
    }

    private fun followPlayerRegularMovement(
        playerPosition: Vector3,
        deltaTime: Float
    ) {
        val physicsComponent = ComponentsMapper.physics.get(gameSessionData.gamePlayData.player) ?: return

        val linearVelocity =
            auxVector3_2.set(physicsComponent.rigidBody.linearVelocity)
                .scl(gameSessionData.fpsTarget * deltaTime * 3F)
        cameraTarget =
            playerPosition.add(linearVelocity.x, 0F, linearVelocity.z + Z_OFFSET)
        val camera = gameSessionData.renderData.camera
        cameraTarget.y = camera.position.y
        camera.position.interpolate(cameraTarget, 0.025F, Interpolation.exp5)
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
    }

}
