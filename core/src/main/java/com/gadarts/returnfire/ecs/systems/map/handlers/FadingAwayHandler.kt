package com.gadarts.returnfire.ecs.systems.map.handlers

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.gadarts.returnfire.ecs.components.ComponentsMapper

class FadingAwayHandler(private val engine: PooledEngine) {
    private val entitiesToFadeAway: MutableList<Entity> = ArrayDeque()
    private val entities: MutableList<Entity> = ArrayDeque()

    fun add(entity: Entity) {
        entities.add(entity)
        ComponentsMapper.modelInstance.get(entity).gameModelInstance.modelInstance.materials.get(0).set(
            BlendingAttribute(1F)
        )
        if (entities.size > 16) {
            entitiesToFadeAway.add(entities.removeFirst())
        }
    }

    fun update(deltaTime: Float) {
        if (entitiesToFadeAway.isNotEmpty()) {
            val first = entitiesToFadeAway.first()
            val modelInstanceComponent = ComponentsMapper.modelInstance.get(first)
            if (modelInstanceComponent == null) {
                entitiesToFadeAway.removeFirst()
            } else {
                val attribute = modelInstanceComponent.gameModelInstance.modelInstance.materials.get(0)
                val blending = attribute.get(BlendingAttribute.Type)
                if (attribute == null || blending == null) {
                    deleteEntity()
                    return
                }
                val blendingAttribute = blending as BlendingAttribute
                if (blendingAttribute.opacity > 0) {
                    blendingAttribute.opacity -= deltaTime
                } else {
                    deleteEntity()
                }
            }
        }
    }

    private fun deleteEntity() {
        val entity = entitiesToFadeAway.removeFirst()
        engine.removeEntity(entity)
    }

}
