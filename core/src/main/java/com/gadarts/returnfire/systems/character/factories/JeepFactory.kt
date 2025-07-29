package com.gadarts.returnfire.systems.character.factories

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.arm.ArmComponent
import com.gadarts.returnfire.components.arm.ArmEffectsData
import com.gadarts.returnfire.components.arm.ArmProperties
import com.gadarts.returnfire.components.arm.ArmRenderData
import com.gadarts.returnfire.components.bullet.BulletBehavior
import com.gadarts.returnfire.components.character.CharacterColor
import com.gadarts.returnfire.factories.GameModelInstanceFactory
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.assets.definitions.ParticleEffectDefinition
import com.gadarts.shared.assets.definitions.SoundDefinition
import com.gadarts.shared.assets.definitions.model.ModelDefinition
import com.gadarts.shared.assets.map.GameMapPlacedObject
import com.gadarts.shared.model.definitions.SimpleCharacterDefinition

class JeepFactory(
    private val assetsManager: GameAssetManager,
    private val gameSessionData: GameSessionData,
    private val entityBuilder: EntityBuilder,
    gameModelInstanceFactory: GameModelInstanceFactory,
) : CharacterFactory(gameModelInstanceFactory, entityBuilder, assetsManager) {
    override fun create(base: GameMapPlacedObject, color: CharacterColor): Entity {
        val primarySpark = addSpark(
            assetsManager.getAssetByDefinition(ModelDefinition.CANNON_SPARK),
            tankPrimaryRelativePositionCalculator
        )
        val entityBuilder = entityBuilder.begin()
        addCharacterBaseComponents(
            base,
            SimpleCharacterDefinition.JEEP,
            primarySpark,
            null,
            {
                addJeepPrimaryArmComponent(entityBuilder, primarySpark)
            },
            null,
            null,
            color
        )
        entityBuilder.addFrontWheelsComponent(ModelDefinition.JEEP_WHEEL)
        entityBuilder.addAmbSoundComponent(assetsManager.getAssetByDefinition(SoundDefinition.ENGINE_LIGHT))
        val character = entityBuilder.finish()
        return character
    }

    override fun dispose() {

    }

    private fun addJeepPrimaryArmComponent(
        entityBuilder: EntityBuilder,
        primarySpark: Entity,
    ): EntityBuilder {
        entityBuilder.addPrimaryArmComponent(
            primarySpark,
            ArmProperties(
                30F,
                assetsManager.getAssetByDefinition(SoundDefinition.CANNON_A),
                TANK_PRI_RELOAD_DUR,
                TANK_PRI_BULLET_SPEED,
                ArmEffectsData(
                    ParticleEffectDefinition.EXPLOSION,
                    null,
                    gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SPARK_SMALL),
                    gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE_UP_LOOP)
                ),
                ArmRenderData(
                    ModelDefinition.TANK_CANNON_BULLET,
                    assetsManager.getCachedBoundingBox(ModelDefinition.TANK_CANNON_BULLET),
                ),
                true,
                gameSessionData.gamePlayData.pools.rigidBodyPools.obtainRigidBodyPool(ModelDefinition.TANK_CANNON_BULLET),
                40,
                AimingRestriction.ONLY_GROUND
            ),
            BulletBehavior.REGULAR
        )
        return entityBuilder
    }

    private val tankPrimaryRelativePositionCalculator = object : ArmComponent.RelativePositionCalculator {
        override fun calculate(parent: Entity, output: Vector3): Vector3 {
            val turret = ComponentsMapper.turretBase.get(parent).turret
            val transform =
                ComponentsMapper.modelInstance.get(turret).gameModelInstance.modelInstance.transform
            val pos = output.set(0.7F, 0F, 0F).rot(transform)
            pos.y += 0.1F
            return pos
        }
    }

    companion object {
        private const val TANK_PRI_RELOAD_DUR = 2000L
        private const val TANK_PRI_BULLET_SPEED = 8F

    }
}
