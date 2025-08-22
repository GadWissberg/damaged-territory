package com.gadarts.returnfire.ecs.systems.map.react

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.ecs.systems.HandlerOnEvent
import com.gadarts.returnfire.ecs.systems.data.GameSessionData
import com.gadarts.returnfire.ecs.systems.map.MapSystemImpl
import com.gadarts.returnfire.ecs.systems.map.MapSystemImpl.ExplosionPushBackEffect
import com.gadarts.returnfire.ecs.systems.map.MapSystemRelatedEntities

class MapSystemOnExplosionPushBack(private val mapSystemRelatedEntities: MapSystemRelatedEntities) : HandlerOnEvent {
    override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        val position = msg.extraInfo as Vector3
        applyExplosionPushBackOnEnvironment(
            position,
            mapSystemRelatedEntities.flyingPartEntities,
            MapSystemImpl.FlyingPartExplosionPushBackEffect
        )
        applyExplosionPushBackOnEnvironment(
            position,
            mapSystemRelatedEntities.treeEntities,
            MapSystemImpl.TreeExplosionPushBackEffect
        )
    }

    private fun applyExplosionPushBackOnEnvironment(
        position: Vector3,
        entities: ImmutableArray<Entity>,
        effect: ExplosionPushBackEffect
    ) {
        for (entity in entities) {
            val touchedEntityPosition =
                ComponentsMapper.modelInstance.get(entity).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector1
                )
            if (touchedEntityPosition.dst2(position) < 3F) {
                effect.go(entity, touchedEntityPosition, position)
            }
        }
    }

    companion object {
        private val auxVector1 = Vector3()
    }
}
