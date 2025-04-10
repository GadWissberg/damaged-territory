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
import com.badlogic.gdx.graphics.g3d.particles.emitters.RegularEmitter
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.DeathSequenceComponent
import com.gadarts.returnfire.components.effects.ParticleEffectComponent
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.utils.GeneralUtils


class EffectsSystem(gamePlayManagers: GamePlayManagers) : GameEntitySystem(gamePlayManagers) {
    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> =
        mapOf(
            SystemEvents.PARTICLE_EFFECTS_COMPONENTS_ADDED_MANUALLY to object : HandlerOnEvent {
                override fun react(
                    msg: Telegram,
                    gameSessionData: GameSessionData,
                    gamePlayManagers: GamePlayManagers
                ) {
                    val particleEffectComponent = ComponentsMapper.particleEffect.get(msg.extraInfo as Entity)
                    updatePositionToParent(particleEffectComponent)
                    playParticleEffect(particleEffectComponent.effect)
                }
            }
        )

    private val particleEffectsEntities: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(
            Family.all(
                ParticleEffectComponent::class.java,
            ).get()
        )
    }

    private val deathSequenceEntities: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(
            Family.all(
                DeathSequenceComponent::class.java
            ).get()
        )
    }

    private val billboardParticleBatch: BillboardParticleBatch by lazy { BillboardParticleBatch() }

    override fun initialize(gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        super.initialize(gameSessionData, gamePlayManagers)
        this.gameSessionData.renderData.particleSystem = ParticleSystem()
        billboardParticleBatch.blendingAttribute.sourceFunction = GL20.GL_SRC_ALPHA
        billboardParticleBatch.blendingAttribute.destFunction = GL20.GL_ONE_MINUS_SRC_ALPHA
        gamePlayManagers.assetsManager.loadParticleEffects(billboardParticleBatch)
        engine.addEntityListener(createEntityListener())
    }

    override fun onSystemReady() {
        billboardParticleBatch.setCamera(gameSessionData.renderData.camera)
        gameSessionData.renderData.particleSystem.add(billboardParticleBatch)
    }

    override fun resume(delta: Long) {
    }

    override fun update(deltaTime: Float) {
        particleEntitiesToRemove.clear()
        updateParticleEffectsComponents()
        removeParticleEffectsMarkedToBeRemoved()
        updateDeathSequences()
        updateSystem(deltaTime)
    }

    private fun updateDeathSequences() {
        for (entity in deathSequenceEntities) {
            val deathSequenceComponent = ComponentsMapper.deathSequence.get(entity)
            if (deathSequenceComponent.deathSequenceDuration <= 0) {
                gamePlayManagers.dispatcher.dispatchMessage(
                    SystemEvents.DEATH_SEQUENCE_FINISHED.ordinal,
                    entity
                )
                entity.remove(DeathSequenceComponent::class.java)
            } else if (deathSequenceComponent.deathSequenceNextExplosion < TimeUtils.millis()) {
                deathSequenceComponent.incrementDeathSequence()
                val specialEffectsFactory = gamePlayManagers.factories.specialEffectsFactory
                if (ComponentsMapper.character.has(entity)) {
                    specialEffectsFactory.generateExplosionForCharacter(
                        character = entity,
                    )
                } else {
                    if (deathSequenceComponent.createExplosionsAround) {
                        val gameModelInstance = ComponentsMapper.modelInstance.get(entity).gameModelInstance
                        val position = GeneralUtils.getRandomPositionOnBoundingBox(
                            gameModelInstance.getBoundingBox(auxBoundingBox),
                            0.5F
                        )
                        specialEffectsFactory.generateExplosion(
                            position
                        )
                    } else {
                        specialEffectsFactory.generateExplosion(
                            entity
                        )
                    }
                }
            }
        }
    }


    override fun dispose() {
        gamePlayManagers.assetsManager.unloadParticleEffects()
    }

    private fun createEntityListener(): EntityListener {
        return object : EntityListener {
            override fun entityAdded(entity: Entity) {
                if (ComponentsMapper.particleEffect.has(entity)) {
                    val effect: ParticleEffect =
                        ComponentsMapper.particleEffect.get(entity).effect
                    playParticleEffect(effect)
                }
            }

            override fun entityRemoved(entity: Entity) {
                if (ComponentsMapper.particleEffect.has(entity)) {
                    val particleEffectComponent = ComponentsMapper.particleEffect.get(entity)
                    if (particleEffectComponent.followEntity != null) {
                        particleEffectComponent.followEntity = null
                        for (controller in particleEffectComponent.effect.controllers) {
                            (controller.emitter as RegularEmitter).isContinuous = false
                        }
                    }
                }
            }
        }
    }

    private fun playParticleEffect(effect: ParticleEffect) {
        gameSessionData.renderData.particleSystem.add(effect)
        effect.start()
    }

    private val particleEntitiesToRemove = ArrayList<Entity>()


    private fun updateParticleEffectsComponents() {
        for (entity in particleEffectsEntities) {
            val particleEffectComponent = ComponentsMapper.particleEffect.get(entity)
            val parent = particleEffectComponent.followEntity
            val ttlInMillis = particleEffectComponent.ttlInSeconds * 1000L
            val timeToLeave =
                ttlInMillis > 0F && TimeUtils.timeSinceMillis(particleEffectComponent.createdAt) >= ttlInMillis
            val effect = particleEffectComponent.effect
            if ((effect.isComplete) || timeToLeave) {
                if (particleEffectComponent.ttlForComponentOnly) {
                    removeParticleEffect(entity)
                    entity.remove(ParticleEffectComponent::class.java)
                } else {
                    particleEntitiesToRemove.add(entity)
                }
            } else if (parent != null) {
                updatePositionToParent(particleEffectComponent)
            }
        }
    }

    private fun updatePositionToParent(
        particleEffectComponent: ParticleEffectComponent,
    ) {
        val parent = particleEffectComponent.followEntity
        if (ComponentsMapper.modelInstance.has(parent)) {
            auxMatrix1.idt().trn(
                ComponentsMapper.modelInstance.get(parent).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector
                )
            ).translate(particleEffectComponent.followRelativePosition)
            particleEffectComponent.effect.setTransform(auxMatrix1)
        }
    }

    private fun removeParticleEffectsMarkedToBeRemoved() {
        for (entity in particleEntitiesToRemove) {
            removeParticleEffect(entity)
            engine.removeEntity(entity)
        }
    }

    private fun removeParticleEffect(entity: Entity) {
        val particleEffectComponent = ComponentsMapper.particleEffect.get(entity)
        val particleEffect = particleEffectComponent.effect
        particleEffect.reset()
        particleEffectComponent.followEntity = null
        gameSessionData.renderData.particleSystem.remove(particleEffect)
        gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(particleEffectComponent.definition)
            .free(particleEffect)
    }

    private fun updateSystem(deltaTime: Float) {
        gameSessionData.renderData.particleSystem.update(deltaTime)
    }

    companion object {
        private val auxMatrix1 = Matrix4()
        private val auxVector = Vector3()
        private val auxBoundingBox = BoundingBox()
    }
}
