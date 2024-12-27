package com.gadarts.returnfire.systems.character.factories

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.arm.ArmComponent
import com.gadarts.returnfire.components.arm.ArmEffectsData
import com.gadarts.returnfire.components.arm.ArmProperties
import com.gadarts.returnfire.components.arm.ArmRenderData
import com.gadarts.returnfire.components.bullet.BulletBehavior
import com.gadarts.returnfire.components.character.CharacterColor
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.factories.GameModelInstanceFactory
import com.gadarts.returnfire.managers.GameAssetManager
import com.gadarts.returnfire.model.PlacedElement
import com.gadarts.returnfire.model.TurretCharacterDefinition
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.data.GameSessionData

class TankFactory(
    private val assetsManager: GameAssetManager,
    private val gameSessionData: GameSessionData,
    private val entityBuilder: EntityBuilder,
    gameModelInstanceFactory: GameModelInstanceFactory,
) : CharacterFactory(gameModelInstanceFactory, entityBuilder) {
    override fun create(base: PlacedElement, color: CharacterColor): Entity {
        val primarySpark = addSpark(
            assetsManager.getAssetByDefinition(ModelDefinition.CANNON_SPARK),
            tankPrimaryRelativePositionCalculator
        )
        val secondarySpark = addSpark(
            assetsManager.getAssetByDefinition(ModelDefinition.CANNON_SPARK),
            tankSecondaryRelativePositionCalculator
        )
        val entityBuilder = entityBuilder.begin()
        addCharacterBaseComponents(
            base,
            TurretCharacterDefinition.TANK,
            primarySpark,
            secondarySpark,
            {
                addTankPrimaryArmComponent(entityBuilder, primarySpark)
            },
            {
                addTankSecondaryArmComponent(entityBuilder, secondarySpark)
            },
            null,
            color
        )
        entityBuilder.addTurretBaseComponent()
        entityBuilder.addAmbSoundComponent(assetsManager.getAssetByDefinition(SoundDefinition.ENGINE))
        val player = entityBuilder.finish()
        val cannon = addTankCannon(player)
        addTurret(player, cannon)
        return player
    }

    private fun addTurret(
        player: Entity,
        cannon: Entity
    ) {
        entityBuilder.begin()
        entityBuilder.addModelInstanceComponent(
            GameModelInstance(
                ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.TANK_TURRET)),
                ModelDefinition.TANK_TURRET
            ),
            ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector3_1
            ),
            null
        )
        entityBuilder.addTurretComponent(player, true, cannon)
        entityBuilder.addChildModelInstanceComponent(
            gameModelInstanceFactory.createGameModelInstance(ModelDefinition.TANK_MISSILE_LAUNCHER),
            true,
            Vector3(-0.1F, 0.1F, -0.05F)
        )
        val turret = entityBuilder.finishAndAddToEngine()
        ComponentsMapper.turretBase.get(player).turret = turret
    }

    private fun addTankPrimaryArmComponent(
        entityBuilder: EntityBuilder,
        primarySpark: Entity,
    ): EntityBuilder {
        entityBuilder.addPrimaryArmComponent(
            primarySpark,
            ArmProperties(
                10,
                assetsManager.getAssetByDefinition(SoundDefinition.MACHINE_GUN),
                TANK_PRI_RELOAD_DUR,
                TANK_PRI_BULLET_SPEED,
                ArmEffectsData(
                    ParticleEffectDefinition.EXPLOSION,
                    null,
                    gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SPARK_SMALL),
                    gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE_UP_LOOP)
                ),
                ArmRenderData(
                    ModelDefinition.TANK_CANNON_BULLET,
                    assetsManager.getCachedBoundingBox(ModelDefinition.TANK_CANNON_BULLET),
                ),
                true,
                gameSessionData.pools.rigidBodyPools.obtainRigidBodyPool(ModelDefinition.TANK_CANNON_BULLET),
            ),
            BulletBehavior.REGULAR
        )
        return entityBuilder
    }

    private fun addTankSecondaryArmComponent(
        entityBuilder: EntityBuilder,
        spark: Entity,
    ): EntityBuilder {
        entityBuilder.addSecondaryArmComponent(
            spark,
            ArmProperties(
                5,
                assetsManager.getAssetByDefinition(SoundDefinition.MISSILE),
                TANK_SEC_RELOAD_DUR,
                TANK_SEC_BULLET_SPEED,
                ArmEffectsData(
                    ParticleEffectDefinition.EXPLOSION,
                    null,
                    gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SPARK_SMALL),
                    gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE_UP_LOOP)
                ),
                ArmRenderData(
                    ModelDefinition.MISSILE,
                    assetsManager.getCachedBoundingBox(ModelDefinition.MISSILE),
                    45F
                ),
                true,
                gameSessionData.pools.rigidBodyPools.obtainRigidBodyPool(ModelDefinition.MISSILE),
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

    private val tankSecondaryRelativePositionCalculator = object : ArmComponent.RelativePositionCalculator {
        override fun calculate(parent: Entity, output: Vector3): Vector3 {
            val turret = ComponentsMapper.turretBase.get(parent).turret
            val transform =
                ComponentsMapper.childModelInstance.get(turret).gameModelInstance.modelInstance.transform
            val pos = output.set(-0.1F, 0.4F, -0.05F).rot(transform)
            return pos
        }
    }

    private fun addTankCannon(
        player: Entity
    ): Entity {
        entityBuilder.begin()
        entityBuilder.addModelInstanceComponent(
            GameModelInstance(
                ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.TANK_CANNON)),
                ModelDefinition.TANK_CANNON
            ),
            ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector3_1
            ),
            null
        )
        val cannon = entityBuilder.finishAndAddToEngine()
        return cannon
    }

    companion object {
        private val auxVector3_1 = Vector3()
        private const val TANK_PRI_RELOAD_DUR = 2000L
        private const val TANK_SEC_RELOAD_DUR = 1500L
        private const val TANK_PRI_BULLET_SPEED = 8F
        private const val TANK_SEC_BULLET_SPEED = 10F

    }
}
