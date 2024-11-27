package com.gadarts.returnfire.systems.player.react

import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btBoxShape
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject.CollisionFlags
import com.badlogic.gdx.physics.bullet.collision.btCompoundShape
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.model.SimpleCharacterDefinition
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData


class PlayerSystemOnCharacterOnboarded :
    HandlerOnEvent {

    override fun react(msg: Telegram, gameSessionData: GameSessionData, managers: Managers) {
        val characterDefinition = ComponentsMapper.character.get(gameSessionData.player).definition
        val isApache = characterDefinition == SimpleCharacterDefinition.APACHE
        val playerShape =
            if (isApache) createApacheCollisionShape() else btBoxShape(
                Vector3(0.5F, 0.15F, 0.35F)
            )
        val modelInstanceComponent = ComponentsMapper.modelInstance.get(gameSessionData.player)
        val modelInstanceTransform =
            modelInstanceComponent.gameModelInstance.modelInstance.transform
        val physicsTransform =
            if (isApache) Matrix4(modelInstanceTransform) else modelInstanceTransform
        val physicsComponent = EntityBuilder.addPhysicsComponent(
            gameSessionData.player,
            playerShape,
            10F,
            managers,
            CollisionFlags.CF_CHARACTER_OBJECT,
            physicsTransform,
        )
        physicsComponent.rigidBody.gravity = characterDefinition.getGravity(Vector3())
        physicsComponent.rigidBody.setDamping(
            if (isApache) 0F else 0.1F,
            if (isApache) 0.75F else 0.99F
        )
        if (!isApache) {
            physicsComponent.rigidBody.friction = 0F
        }
        physicsComponent.rigidBody.angularFactor = if (isApache) Vector3.Y else Vector3.Zero
        physicsComponent.rigidBody.linearFactor = characterDefinition.getLinearFactor(Vector3())
    }

    private fun createApacheCollisionShape(): btCompoundShape {
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
