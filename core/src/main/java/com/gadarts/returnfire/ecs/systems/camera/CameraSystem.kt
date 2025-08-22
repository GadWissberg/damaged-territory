package com.gadarts.returnfire.ecs.systems.camera

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.ecs.systems.GameEntitySystem
import com.gadarts.returnfire.ecs.systems.HandlerOnEvent
import com.gadarts.returnfire.ecs.systems.data.GameSessionData
import com.gadarts.returnfire.ecs.systems.events.SystemEvents

class CameraSystem(gamePlayManagers: GamePlayManagers) : GameEntitySystem(gamePlayManagers) {

    private val cameraMovementHandler by lazy { CameraMovementHandler(gameSessionData) }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> =
        mapOf(
            SystemEvents.PLAYER_ADDED to object : HandlerOnEvent {
                override fun react(
                    msg: Telegram,
                    gameSessionData: GameSessionData,
                    gamePlayManagers: GamePlayManagers
                ) {
                    cameraMovementHandler.init()
                }
            },
            SystemEvents.CHARACTER_DEPLOYED to object : HandlerOnEvent {
                override fun react(
                    msg: Telegram,
                    gameSessionData: GameSessionData,
                    gamePlayManagers: GamePlayManagers
                ) {
                    if (!ComponentsMapper.player.has(msg.extraInfo as Entity)) return

                    cameraMovementHandler.onCharacterDeployed()
                }
            })


    override fun update(deltaTime: Float) {
        cameraMovementHandler.update(deltaTime)
        gameSessionData.renderData.camera.update()
    }

    override fun resume(delta: Long) {
    }

    override fun dispose() {
    }









}
