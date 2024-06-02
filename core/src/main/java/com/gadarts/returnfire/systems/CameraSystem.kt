package com.gadarts.returnfire.systems

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.Services
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.PlayerComponent
import com.gadarts.returnfire.systems.events.SystemEvents

class CameraSystem : GameEntitySystem() {


    private var cameraStrafeMode = Vector3().setZero()
    private var cameraTarget = Vector3()

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = emptyMap()

    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        gameSessionData.camera.update()
        followPlayer()
    }

    override fun resume(delta: Long) {
    }

    override fun dispose() {
    }

    override fun initialize(gameSessionData: GameSessionData, services: Services) {
        super.initialize(gameSessionData, services)
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
        val playerComp = ComponentsMapper.player.get(player)
        if (playerComp.strafing == null) {
            followPlayerRegularMovement(playerComp, playerPosition)
        } else {
            followPlayerWhenStrafing(playerPosition)
        }
    }

    private fun followPlayerWhenStrafing(playerPosition: Vector3) {
        if (cameraStrafeMode.isZero) {
            cameraStrafeMode.set(gameSessionData.camera.position).sub(playerPosition)
        } else {
            gameSessionData.camera.position.set(
                playerPosition.x,
                gameSessionData.camera.position.y,
                playerPosition.z
            ).add(cameraStrafeMode.x, 0F, cameraStrafeMode.z)
        }
    }
    private fun followPlayerRegularMovement(
        playerComp: PlayerComponent,
        playerPosition: Vector3
    ) {
        val velocityDir = playerComp.getCurrentVelocity(auxVector2).nor().setLength2(5F)
        cameraTarget =
            playerPosition.add(velocityDir.x, 0F, -velocityDir.y + Z_OFFSET)
        cameraTarget.y = gameSessionData.camera.position.y
        gameSessionData.camera.position.interpolate(cameraTarget, 0.2F, Interpolation.exp5)
        if (!cameraStrafeMode.isZero) {
            cameraStrafeMode.setZero()
        }
    }


    private fun initializeCamera() {
        gameSessionData.camera.near = NEAR
        gameSessionData.camera.far = FAR
        gameSessionData.camera.update()
        gameSessionData.camera.position.set(0F, INITIAL_Y, Z_OFFSET)
        gameSessionData.camera.rotate(Vector3.X, -45F)
        val get = ComponentsMapper.modelInstance.get(gameSessionData.player)
        gameSessionData.camera.lookAt(
            get.gameModelInstance.modelInstance.transform.getTranslation(
                auxVector3_1
            )
        )
    }

    companion object {
        const val NEAR = 0.1F
        const val FAR = 300F
        const val INITIAL_Y = 8F
        const val Z_OFFSET = 4F
        val auxVector2 = Vector2()
        val auxVector3_1 = Vector3()
    }

}
