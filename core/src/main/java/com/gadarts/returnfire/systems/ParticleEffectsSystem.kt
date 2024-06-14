package com.gadarts.returnfire.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem
import com.badlogic.gdx.graphics.g3d.particles.batches.BillboardParticleBatch
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.components.BaseParticleEffectComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.FollowerParticleEffectComponent
import com.gadarts.returnfire.components.IndependentParticleEffectComponent
import com.gadarts.returnfire.systems.events.SystemEvents


class ParticleEffectsSystem : GameEntitySystem() {
    private lateinit var particleEffectsEntities: ImmutableArray<Entity>

    private lateinit var billboardParticleBatch: BillboardParticleBatch

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> =
        mapOf(SystemEvents.BUILDING_DESTROYED to object :
            HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                managers: Managers
            ) {
                managers.soundPlayer.play(
                    managers.assetsManager.getAssetByDefinition(
                        SoundDefinition.EXPLOSION
                    )
                )
                EntityBuilder.begin().addParticleEffectComponent(
                    managers.assetsManager.getAssetByDefinition(ParticleEffectDefinition.EXPLOSION_GROUND),
                    ComponentsMapper.modelInstance.get(msg.extraInfo as Entity).gameModelInstance.modelInstance.transform.getTranslation(
                        auxVector1
                    )
                ).finishAndAddToEngine()
            }
        })

    override fun resume(delta: Long) {
    }

    override fun onSystemReady() {
        billboardParticleBatch.setCamera(gameSessionData.camera)
        gameSessionData.particleSystem.add(billboardParticleBatch)
    }

    override fun initialize(gameSessionData: GameSessionData, managers: Managers) {
        super.initialize(gameSessionData, managers)
        this.gameSessionData.particleSystem = ParticleSystem()
        billboardParticleBatch = BillboardParticleBatch()
        billboardParticleBatch.blendingAttribute.sourceFunction = GL20.GL_SRC_ALPHA
        billboardParticleBatch.blendingAttribute.destFunction = GL20.GL_ONE_MINUS_SRC_ALPHA
        managers.assetsManager.loadParticleEffects(billboardParticleBatch)
        particleEffectsEntities = engine.getEntitiesFor(
            Family.one(
                IndependentParticleEffectComponent::class.java,
                FollowerParticleEffectComponent::class.java
            ).get()
        )
        engine.addEntityListener(createEntityListener())
    }

    private fun createEntityListener(): EntityListener {
        return object : EntityListener {
            override fun entityAdded(entity: Entity?) {
                if (ComponentsMapper.independentParticleEffect.has(entity)) {
                    val effect: ParticleEffect =
                        ComponentsMapper.independentParticleEffect.get(entity).effect
                    playParticleEffect(effect)
                }
            }

            override fun entityRemoved(entity: Entity?) {
            }
        }
    }

    private fun playParticleEffect(effect: ParticleEffect) {
        gameSessionData.particleSystem.add(effect)
        effect.init()
        effect.start()
    }

    override fun update(deltaTime: Float) {
        updateSystem(deltaTime)
        handleCompletedParticleEffects()
    }

    private val particleEntitiesToRemove = ArrayList<Entity>()

    private fun handleCompletedParticleEffects() {
        particleEntitiesToRemove.clear()
        updateParticleEffectsComponents()
        removeParticleEffectsMarkedToBeRemoved()
    }

    private fun updateParticleEffectsComponents() {
        for (entity in particleEffectsEntities) {
            val particleEffectComponent = fetchParticleEffect(entity)
            val particleEffect: ParticleEffect =
                particleEffectComponent.effect
            if (particleEffect.isComplete) {
                particleEntitiesToRemove.add(entity)
            } else if (ComponentsMapper.followerParticleEffect.has(entity)) {
                updateFollower(entity, particleEffectComponent)
            }
        }
    }

    private fun updateFollower(
        entity: Entity,
        particleEffectComponent: BaseParticleEffectComponent
    ) {
        ComponentsMapper.modelInstance.get(entity).gameModelInstance.modelInstance.transform.getTranslation(
            auxVector1
        )
        auxMatrix.setToTranslation(auxVector1)
        particleEffectComponent.effect.setTransform(auxMatrix)
    }

    private fun fetchParticleEffect(entity: Entity): BaseParticleEffectComponent =
        if (ComponentsMapper.independentParticleEffect.has(entity)) ComponentsMapper.independentParticleEffect.get(
            entity
        ) else ComponentsMapper.followerParticleEffect.get(entity)

    private fun removeParticleEffectsMarkedToBeRemoved() {
        for (entity in particleEntitiesToRemove) {
            gameSessionData.particleSystem.remove(fetchParticleEffect(entity).effect)
            entity.remove(BaseParticleEffectComponent::class.java)
        }
    }

    private fun updateSystem(deltaTime: Float) {
        gameSessionData.particleSystem.update(deltaTime)
        gameSessionData.particleSystem.begin()
        gameSessionData.particleSystem.draw()
        gameSessionData.particleSystem.end()
    }

    override fun dispose() {
    }

    companion object {
        private val auxVector1 = Vector3()
        private val auxMatrix = Matrix4()
    }
}
