package com.gadarts.returnfire.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem
import com.badlogic.gdx.graphics.g3d.particles.batches.BillboardParticleBatch
import com.badlogic.gdx.graphics.g3d.particles.emitters.RegularEmitter
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.ParticleEffectComponent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents


class ParticleEffectsSystem : GameEntitySystem() {
    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> =
        mapOf()

    private val particleEffectsEntities: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(
            Family.all(
                ParticleEffectComponent::class.java,
            ).get()
        )
    }

    private val billboardParticleBatch: BillboardParticleBatch by lazy { BillboardParticleBatch() }

    override fun resume(delta: Long) {
    }

    override fun onSystemReady() {
        billboardParticleBatch.setCamera(gameSessionData.renderData.camera)
        gameSessionData.renderData.particleSystem.add(billboardParticleBatch)
    }

    override fun initialize(gameSessionData: GameSessionData, managers: Managers) {
        super.initialize(gameSessionData, managers)
        this.gameSessionData.renderData.particleSystem = ParticleSystem()
        billboardParticleBatch.blendingAttribute.sourceFunction = GL20.GL_SRC_ALPHA
        billboardParticleBatch.blendingAttribute.destFunction = GL20.GL_ONE_MINUS_SRC_ALPHA
        managers.assetsManager.loadParticleEffects(billboardParticleBatch)
        engine.addEntityListener(createEntityListener())
    }

    override fun update(deltaTime: Float) {
        updateSystem(deltaTime)
        particleEntitiesToRemove.clear()
        updateParticleEffectsComponents()
        removeParticleEffectsMarkedToBeRemoved()
    }

    override fun dispose() {
        managers.assetsManager.unloadParticleEffects()
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
                    if (particleEffectComponent.parent != null) {
                        particleEffectComponent.parent = null
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
            val parent = particleEffectComponent.parent
            val ttlInMillis = particleEffectComponent.ttlInSeconds * 1000L
            val timeToLeave =
                ttlInMillis > 0F && TimeUtils.timeSinceMillis(particleEffectComponent.createdAt) >= ttlInMillis
            val effect = particleEffectComponent.effect
            if ((particleEffectComponent.parent == null && effect.isComplete)
                || (parent != null
                        && ComponentsMapper.character.has(parent)
                        && ComponentsMapper.character.get(parent).dead)
                || timeToLeave
                || (parent != null && ComponentsMapper.modelInstance.get(parent).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector1
                ).y <= -1F)
            ) {
                particleEntitiesToRemove.add(entity)
                return
            } else if (parent != null) {
                auxMatrix.set(ComponentsMapper.modelInstance.get(parent).gameModelInstance.modelInstance.transform)
                    .translate(particleEffectComponent.parentRelativePosition)
                effect.setTransform(auxMatrix)
            }
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
        particleEffectComponent.parent = null
        gameSessionData.renderData.particleSystem.remove(particleEffect)
        gameSessionData.pools.particleEffectsPools.obtain(particleEffectComponent.definition)
            .free(particleEffect)
    }

    private fun updateSystem(deltaTime: Float) {
        gameSessionData.renderData.particleSystem.update(deltaTime)
        if (!GameDebugSettings.AVOID_PARTICLE_EFFECTS_DRAWING) {
            gameSessionData.renderData.particleSystem.begin()
            gameSessionData.renderData.particleSystem.draw()
            gameSessionData.renderData.particleSystem.end()
        }
    }

    companion object {
        private val auxVector1 = Vector3()
        private val auxMatrix = Matrix4()
    }
}
