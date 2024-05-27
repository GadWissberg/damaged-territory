package com.gadarts.returnfire.systems.bullet

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.gadarts.returnfire.Services
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.bullet.BulletBehavior
import com.gadarts.returnfire.components.bullet.BulletComponent
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.GameSessionData
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.map.MapUtils

class BulletSystem : GameEntitySystem() {
    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = emptyMap()
    private lateinit var bulletEntities: ImmutableArray<Entity>
    override fun initialize(gameSessionData: GameSessionData, services: Services) {
        super.initialize(gameSessionData, services)
        bulletEntities = engine.getEntitiesFor(Family.all(BulletComponent::class.java).get())
    }

    private fun takeStepForBullet(
        bullet: Entity,
        deltaTime: Float
    ) {
        val bulletComponent = ComponentsMapper.bullet.get(bullet)
        val modelInstance = ComponentsMapper.modelInstance.get(bullet).gameModelInstance
        if (bulletComponent.behavior == BulletBehavior.CURVE) {
            modelInstance.modelInstance.transform.rotate(Vector3.Z, -1F)
        }
        val speed = bulletComponent.speed
        val prevPosition = modelInstance.modelInstance.transform.getTranslation(auxVector)
        val prevRow = prevPosition.z.toInt() / GameSessionData.REGION_SIZE
        val prevCol = prevPosition.x.toInt() / GameSessionData.REGION_SIZE
        modelInstance.modelInstance.transform.trn(
            getDirectionOfModel(bullet).nor().scl(speed * deltaTime)
        )
        modelInstance.updateBoundingBoxPosition()
        if (gameSessionData.entitiesAcrossRegions[prevRow][prevCol] != null) {
            val bulletBoundingBox =
                ComponentsMapper.modelInstance.get(bullet).gameModelInstance.getBoundingBox(
                    auxBoundingBox1
                )
            for (entity in gameSessionData.entitiesAcrossRegions[prevRow][prevCol]!!) {
                val entityBoundingBox =
                    ComponentsMapper.modelInstance.get(entity).gameModelInstance.getBoundingBox(
                        auxBoundingBox2
                    )
                if (bulletBoundingBox.intersects(entityBoundingBox)) {
                    engine.removeEntity(bullet)
                    engine.removeEntity(entity)
                    break
                }
            }
        }
        MapUtils.notifyEntityRegionChanged(
            modelInstance.modelInstance.transform.getTranslation(auxVector),
            prevRow,
            prevCol,
            services.dispatcher
        )
    }

    private fun getDirectionOfModel(entity: Entity): Vector3 {
        val transform =
            ComponentsMapper.modelInstance.get(entity).gameModelInstance.modelInstance.transform
        auxVector.set(1F, 0F, 0F).rot(transform)
        return auxVector
    }

    private fun updateBullets(deltaTime: Float) {
        for (bullet in bulletEntities) {
            takeStepForBullet(bullet, deltaTime)
            val transform =
                ComponentsMapper.modelInstance.get(bullet).gameModelInstance.modelInstance.transform
            val currentPosition = transform.getTranslation(auxVector)
            val dst = ComponentsMapper.bullet.get(bullet).initialPosition.dst2(currentPosition)
            if (dst > BULLET_MAX_DISTANCE || currentPosition.y <= 0) {
                val pooledEngine = engine as PooledEngine
                pooledEngine.removeEntity(bullet)
            }
        }
    }

    override fun update(deltaTime: Float) {
        updateBullets(deltaTime)
    }

    override fun resume(delta: Long) {
    }

    override fun dispose() {
    }

    companion object {
        private val auxVector = Vector3()
        private val auxBoundingBox1 = BoundingBox()
        private val auxBoundingBox2 = BoundingBox()
        private val auxQuat = Quaternion()
        private const val BULLET_MAX_DISTANCE = 100F
        private const val BULLET_TILT_BIAS = 0.5F
    }
}
