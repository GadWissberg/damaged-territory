package com.gadarts.returnfire.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem
import com.badlogic.gdx.graphics.g3d.particles.batches.BillboardParticleBatch
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.ParticleEffectComponent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents


class ParticleEffectsSystem : GameEntitySystem() {
    private val particleEffectsEntities: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(
            Family.all(
                ParticleEffectComponent::class.java,
            ).get()
        )
    }

    private val billboardParticleBatch: BillboardParticleBatch by lazy { BillboardParticleBatch() }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> =
        mapOf()

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
                        removeParticleEffect(entity)
                    }
                }
            }
        }
    }

    private fun playParticleEffect(effect: ParticleEffect) {
        gameSessionData.renderData.particleSystem.add(effect)
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
            val particleEffectComponent = ComponentsMapper.particleEffect.get(entity)
            val particleEffect: ParticleEffect =
                particleEffectComponent.effect
            val parent = particleEffectComponent.parent
            if ((!particleEffectComponent.definition.loop && particleEffect.isComplete)
                || (parent != null
                    && ComponentsMapper.character.has(parent)
                    && ComponentsMapper.character.get(parent).dead)
            ) {
                particleEntitiesToRemove.add(entity)
            } else if (parent != null) {
                val parentTransform =
                    ComponentsMapper.modelInstance.get(parent).gameModelInstance.modelInstance.transform
                parentTransform.getTranslation(
                    auxVector1
                )
                auxMatrix.setToTranslation(auxVector1).trn(particleEffectComponent.parentRelativePosition)
                particleEffectComponent.effect.setTransform(auxMatrix)
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
        gameSessionData.renderData.particleSystem.remove(particleEffect)
        gameSessionData.pools.particleEffectsPools.obtain(particleEffectComponent.definition)
            .free(particleEffect)
    }

    private fun updateSystem(deltaTime: Float) {
        gameSessionData.renderData.particleSystem.update(deltaTime)
        gameSessionData.renderData.particleSystem.begin()
        gameSessionData.renderData.particleSystem.draw()
        gameSessionData.renderData.particleSystem.end()
    }

    override fun dispose() {
    }

    companion object {
        private val auxVector1 = Vector3()
        private val auxMatrix = Matrix4()
    }
}
