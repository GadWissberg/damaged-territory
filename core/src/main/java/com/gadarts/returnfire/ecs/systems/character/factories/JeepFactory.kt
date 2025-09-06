package com.gadarts.returnfire.ecs.systems.character.factories

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.components.arm.ArmComponent
import com.gadarts.returnfire.ecs.components.arm.ArmEffectsData
import com.gadarts.returnfire.ecs.components.arm.ArmProperties
import com.gadarts.returnfire.ecs.components.arm.ArmRenderData
import com.gadarts.returnfire.ecs.components.bullet.BulletBehavior
import com.gadarts.shared.data.CharacterColor
import com.gadarts.returnfire.factories.GameModelInstanceFactory
import com.gadarts.returnfire.ecs.systems.EntityBuilder
import com.gadarts.returnfire.ecs.systems.data.GameSessionData
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.assets.definitions.ParticleEffectDefinition
import com.gadarts.shared.assets.definitions.SoundDefinition
import com.gadarts.shared.assets.definitions.model.ModelDefinition
import com.gadarts.shared.data.definitions.TurretCharacterDefinition

class JeepFactory(
    private val assetsManager: GameAssetManager,
    private val gameSessionData: GameSessionData,
    private val entityBuilder: EntityBuilder,
    gameModelInstanceFactory: GameModelInstanceFactory,
) : CharacterFactory(gameModelInstanceFactory, entityBuilder, assetsManager) {
    override fun create(position: Vector3, color: CharacterColor): Entity {
        val primarySpark = addSpark(
            assetsManager.getAssetByDefinition(ModelDefinition.MACHINE_GUN_SPARK),
            jeepPrimaryRelativePositionCalculator
        )
        val entityBuilder = entityBuilder.begin()
        addCharacterBaseComponents(
            position,
            TurretCharacterDefinition.JEEP,
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
        entityBuilder.addTurretBaseComponent()
        val jeep = entityBuilder.finish()
        val cannon = addCannon(jeep)
        addTurret(jeep, cannon)
        applyOpponentColor(jeep, color, "jeep_texture")
        return jeep
    }

    private fun addCannon(
        jeep: Entity
    ): Entity {
        entityBuilder.begin()
        entityBuilder.addModelInstanceComponent(
            gameModelInstanceFactory.createGameModelInstance(ModelDefinition.JEEP_GUN),
            ComponentsMapper.modelInstance.get(jeep).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector3_1
            ),
            null
        )
        entityBuilder.addTurretCannonComponent(0F, 0.1F)
        val cannon = entityBuilder.finishAndAddToEngine()
        return cannon
    }

    private fun addTurret(
        player: Entity,
        cannon: Entity,
    ) {
        entityBuilder.begin()
        entityBuilder.addModelInstanceComponent(
            gameModelInstanceFactory.createGameModelInstance(ModelDefinition.JEEP_TURRET_BASE),
            ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector3_1
            ),
            null
        )
        entityBuilder.addTurretComponent(
            player,
            followBasePosition = true,
            followBaseRotation = true,
            relativeY = 0.4F,
            cannon = cannon
        )
        entityBuilder.addTurretAutomationComponent()
        val turret = entityBuilder.finishAndAddToEngine()
        ComponentsMapper.turretBase.get(player).turret = turret
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
                1F,
                assetsManager.getAssetByDefinition(SoundDefinition.MACHINE_GUN_LIGHT),
                PRI_RELOAD_DUR,
                PRI_BULLET_SPEED,
                ArmEffectsData(
                    ParticleEffectDefinition.RICOCHET,
                    null,
                    gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.RICOCHET),
                    null
                ),
                ArmRenderData(
                    ModelDefinition.BULLET,
                    assetsManager.getCachedBoundingBox(ModelDefinition.BULLET),
                ),
                false,
                gameSessionData.gamePlayData.pools.rigidBodyPools.obtainRigidBodyPool(ModelDefinition.BULLET),
                1000,
                null
            ),
            BulletBehavior.REGULAR
        )
        return entityBuilder
    }

    private val jeepPrimaryRelativePositionCalculator = object : ArmComponent.RelativePositionCalculator {
        override fun calculate(parent: Entity, output: Vector3): Vector3 {
            val turret = ComponentsMapper.turretBase.get(parent).turret
            val transform =
                ComponentsMapper.modelInstance.get(ComponentsMapper.turret.get(turret).cannon).gameModelInstance.modelInstance.transform
            val pos = output.set(0.2F, 0F, 0F).rot(transform)
            return pos
        }
    }

    companion object {
        private const val PRI_RELOAD_DUR = 150L
        private const val PRI_BULLET_SPEED = 16F
        private val auxVector3_1 = Vector3()
    }
}
