package com.gadarts.returnfire.systems.map.react

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.factories.SpecialEffectsFactory
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData

class MapSystemOnPhysicsDrowning : HandlerOnEvent {
    override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        val entity = msg.extraInfo as Entity
        val modelInstanceComponent = ComponentsMapper.modelInstance.get(entity) ?: return
        if (entity.isRemoving || entity.isScheduledForRemoval) return

        val position =
            modelInstanceComponent.gameModelInstance.modelInstance.transform.getTranslation(
                auxVector1
            )
        position.set(
            position.x + MathUtils.randomSign() * MathUtils.random(0.2F),
            SpecialEffectsFactory.WATER_SPLASH_Y,
            position.z + MathUtils.randomSign() * MathUtils.random(0.2F)
        )
        gamePlayManagers.factories.specialEffectsFactory.generateWaterSplash(
            position, ComponentsMapper.character.has(entity)
        )
        val physicsComponent = ComponentsMapper.physics.get(
            entity
        )
        val rigidBody = physicsComponent.rigidBody
        gameSessionData.physicsData.collisionWorld.removeRigidBody(
            rigidBody
        )
        gamePlayManagers.ecs.engine.removeEntity(entity)

    }

    companion object {
        private val auxVector1 = Vector3()
    }
}
