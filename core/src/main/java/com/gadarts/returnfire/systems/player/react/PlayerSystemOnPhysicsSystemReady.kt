package com.gadarts.returnfire.systems.player.react

import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btBoxShape
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents

class PlayerSystemOnPhysicsSystemReady :
    HandlerOnEvent {
    override fun react(msg: Telegram, gameSessionData: GameSessionData, managers: Managers) {
        val boundingBox = managers.assetsManager.getCachedBoundingBox(ModelDefinition.APACHE)
        val boxShape = btBoxShape(
            Vector3(
                boundingBox.width / 2F,
                boundingBox.height / 2F,
                boundingBox.depth / 2F
            )
        )
        val physicsComponent = EntityBuilder.addPhysicsComponent(
            boxShape,
            gameSessionData.gameSessionDataEntities.player,
            Matrix4(ComponentsMapper.modelInstance.get(gameSessionData.gameSessionDataEntities.player).gameModelInstance.modelInstance.transform)
        )
        physicsComponent.rigidBody.setDamping(0F, 0.75F)
        managers.dispatcher.dispatchMessage(SystemEvents.PHYSICS_COMPONENT_ADDED_MANUALLY.ordinal)
    }

}
