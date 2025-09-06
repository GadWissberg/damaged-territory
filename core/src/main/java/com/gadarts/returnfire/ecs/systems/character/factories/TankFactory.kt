package com.gadarts.returnfire.ecs.systems.character.factories

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.components.arm.ArmComponent
import com.gadarts.returnfire.ecs.components.arm.ArmEffectsData
import com.gadarts.returnfire.ecs.components.arm.ArmProperties
import com.gadarts.returnfire.ecs.components.arm.ArmRenderData
import com.gadarts.returnfire.ecs.components.bullet.BulletBehavior
import com.gadarts.shared.data.CharacterColor
import com.gadarts.returnfire.ecs.components.model.GameModelInstance
import com.gadarts.returnfire.ecs.systems.EntityBuilder
import com.gadarts.returnfire.ecs.systems.data.GameSessionData
import com.gadarts.returnfire.factories.GameModelInstanceFactory
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.assets.definitions.ParticleEffectDefinition
import com.gadarts.shared.assets.definitions.SoundDefinition
import com.gadarts.shared.assets.definitions.model.ModelDefinition
import com.gadarts.shared.data.ImmutableGameModelInstanceInfo
import com.gadarts.shared.data.definitions.TurretCharacterDefinition

class TankFactory(
    private val assetsManager: GameAssetManager,
    private val gameSessionData: GameSessionData,
    private val entityBuilder: EntityBuilder,
    gameModelInstanceFactory: GameModelInstanceFactory,
) : CharacterFactory(gameModelInstanceFactory, entityBuilder, assetsManager) {
    override fun create(position: Vector3, color: CharacterColor): Entity {
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
            position,
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
        entityBuilder.addLimitedVelocityComponent(1.1F)
        entityBuilder.addAmbSoundComponent(assetsManager.getAssetByDefinition(SoundDefinition.ENGINE_HEAVY))
        val character = entityBuilder.finish()
        val cannon = addTankCannon(character)
        addTurret(character, cannon, color)
        applyOpponentColor(character, color, "tank_body_texture")
        return character
    }

    override fun dispose() {

    }

    private fun addTurret(
        player: Entity,
        cannon: Entity,
        color: CharacterColor
    ) {
        entityBuilder.begin()
        entityBuilder.addModelInstanceComponent(
            GameModelInstance(
                ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.TANK_TURRET)),
                ImmutableGameModelInstanceInfo(ModelDefinition.TANK_TURRET),
            ),
            ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector3_1
            ),
            null
        )
        entityBuilder.addTurretComponent(player, true, true, 0.2F, cannon)
        entityBuilder.addChildModelInstanceComponent(
            gameModelInstanceFactory.createGameModelInstance(ModelDefinition.TANK_MISSILE_LAUNCHER),
            true,
            Vector3(-0.1F, 0.1F, -0.05F)
        )
        val turret = entityBuilder.finishAndAddToEngine()
        ComponentsMapper.turretBase.get(player).turret = turret
        applyOpponentColor(turret, color, "tank_turret_texture")
    }

    private fun addTankPrimaryArmComponent(
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

    private fun addTankSecondaryArmComponent(
        entityBuilder: EntityBuilder,
        spark: Entity,
    ): EntityBuilder {
        entityBuilder.addSecondaryArmComponent(
            spark,
            ArmProperties(
                20F,
                assetsManager.getAssetByDefinition(SoundDefinition.MISSILE),
                TANK_SEC_RELOAD_DUR,
                TANK_SEC_BULLET_SPEED,
                ArmEffectsData(
                    ParticleEffectDefinition.EXPLOSION,
                    null,
                    gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SPARK_SMALL),
                    gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE_SMALL_LOOP)
                ),
                ArmRenderData(
                    ModelDefinition.MISSILE,
                    assetsManager.getCachedBoundingBox(ModelDefinition.MISSILE),
                    45F
                ),
                true,
                gameSessionData.gamePlayData.pools.rigidBodyPools.obtainRigidBodyPool(ModelDefinition.MISSILE),
                20,
                AimingRestriction.ONLY_SKY,
                false
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
            val pos = output.set(0.4F, 0F, 0F).rot(transform)
            return pos
        }
    }

    private val tankSecondaryRelativePositionCalculator = object : ArmComponent.RelativePositionCalculator {
        override fun calculate(parent: Entity, output: Vector3): Vector3 {
            val turret = ComponentsMapper.turretBase.get(parent).turret
            val transform =
                ComponentsMapper.childModelInstance.get(turret).gameModelInstance.modelInstance.transform
            val pos = output.set(-0.4F, 0.2F, -0.05F).rot(transform)
            return pos
        }
    }

    private fun addTankCannon(
        tank: Entity
    ): Entity {
        entityBuilder.begin()
        entityBuilder.addModelInstanceComponent(
            gameModelInstanceFactory.createGameModelInstance(ModelDefinition.TANK_CANNON),
            ComponentsMapper.modelInstance.get(tank).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector3_1
            ),
            null
        )
        entityBuilder.addTurretCannonComponent(0.31F, 0F)
        val cannon = entityBuilder.finishAndAddToEngine()
        applyOpponentColor(cannon, ComponentsMapper.character.get(tank).color, "tank_cannon_texture")
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
