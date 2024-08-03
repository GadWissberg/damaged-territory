package com.gadarts.returnfire.systems.bullet

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.bullet.BulletBehavior
import com.gadarts.returnfire.components.bullet.BulletComponent
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents

class BulletSystem : GameEntitySystem() {
    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = emptyMap()

    private lateinit var bulletEntities: ImmutableArray<Entity>

    override fun initialize(gameSessionData: GameSessionData, managers: Managers) {
        super.initialize(gameSessionData, managers)
        bulletEntities = engine.getEntitiesFor(Family.all(BulletComponent::class.java).get())
    }

    override fun update(deltaTime: Float) {
        updateBullets(deltaTime)
    }

    override fun resume(delta: Long) {
    }

    override fun dispose() {
    }

    private fun takeStepForBullet(
        bullet: Entity,
        deltaTime: Float
    ) {
        val bulletComponent = ComponentsMapper.bullet.get(bullet)
        val modelInstance = ComponentsMapper.modelInstance.get(bullet).gameModelInstance
        handleBulletSpecialMovement(bulletComponent, modelInstance)
        modelInstance.modelInstance.transform.trn(
            getDirectionOfModel(bullet).nor().scl(bulletComponent.speed * deltaTime)
        )
    }

    private fun handleBulletSpecialMovement(
        bulletComponent: BulletComponent,
        modelInstance: GameModelInstance
    ) {
        if (bulletComponent.behavior == BulletBehavior.CURVE) {
            if (modelInstance.modelInstance.transform.getRotation(auxQuat)
                    .getAngleAround(Vector3.Z) > 270F
            ) {
                modelInstance.modelInstance.transform.rotate(Vector3.Z, -1F)
            }
        }
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

    companion object {
        private val auxVector = Vector3()
        private val auxQuat = Quaternion()
        private const val BULLET_MAX_DISTANCE = 100F
    }
}
