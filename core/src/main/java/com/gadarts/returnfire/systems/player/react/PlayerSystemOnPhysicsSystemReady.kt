package com.gadarts.returnfire.systems.player.react

import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btBoxShape
import com.badlogic.gdx.physics.bullet.collision.btCompoundShape
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents


class PlayerSystemOnPhysicsSystemReady :
    HandlerOnEvent {
    override fun react(msg: Telegram, gameSessionData: GameSessionData, managers: Managers) {
        val playerShape = createCollisionShape()
        val modelInstanceComponent = ComponentsMapper.modelInstance.get(gameSessionData.player)
        val physicsComponent = EntityBuilder.addPhysicsComponent(
            playerShape,
            gameSessionData.player,
            Matrix4(modelInstanceComponent.gameModelInstance.modelInstance.transform)
        )
        physicsComponent.rigidBody.setDamping(0F, 0.75F)
        physicsComponent.rigidBody.angularFactor = Vector3.Y
        managers.dispatcher.dispatchMessage(SystemEvents.PHYSICS_COMPONENT_ADDED_MANUALLY.ordinal)
    }

    private fun createCollisionShape(): btCompoundShape {
        val playerShape = btCompoundShape()
        val bodyShape = btBoxShape(
            Vector3(0.4F, 0.08F, 0.1F)
        )
        val tailShape = btBoxShape(
            Vector3(0.3F, 0.04F, 0.04F)
        )
        val wing = btBoxShape(
            Vector3(0.1F, 0.05F, 0.1F)
        )
        playerShape.addChildShape(
            Matrix4().translate(Vector3(0.05F, -0.15F, 0F)).rotate(Vector3.Z, -15F), bodyShape
        )
        playerShape.addChildShape(
            Matrix4().translate(Vector3(-0.6F, 0F, 0F)).rotate(Vector3.Z, -15F), tailShape
        )
        playerShape.addChildShape(
            Matrix4().translate(Vector3(0F, -0.2F, 0.2F)).rotate(Vector3.Z, -10F), wing
        )
        playerShape.addChildShape(
            Matrix4().translate(Vector3(0F, -0.2F, -0.2F)).rotate(Vector3.Z, -10F), wing
        )
        return playerShape
    }

}
