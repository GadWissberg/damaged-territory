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
import com.gadarts.shared.model.definitions.TurretCharacterDefinition

class JeepFactory(
    private val assetsManager: GameAssetManager,
    private val gameSessionData: GameSessionData,
    private val entityBuilder: EntityBuilder,
    gameModelInstanceFactory: GameModelInstanceFactory,
) : CharacterFactory(gameModelInstanceFactory, entityBuilder, assetsManager) {
    override fun create(base: GameMapPlacedObject, color: CharacterColor): Entity {
        val primarySpark = addSpark(
            assetsManager.getAssetByDefinition(ModelDefinition.MACHINE_GUN_SPARK),
            jeepPrimaryRelativePositionCalculator
        )
        val entityBuilder = entityBuilder.begin()
        addCharacterBaseComponents(
            base,
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
        addTurret(jeep, cannon, color)
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
//        applyOpponentColor(cannon, ComponentsMapper.character.get(jeep).color, "tank_cannon_texture")
        return cannon
    }

    private fun addTurret(
        player: Entity,
        cannon: Entity,
        color: CharacterColor
    ) {
        entityBuilder.begin()
        entityBuilder.addModelInstanceComponent(
            gameModelInstanceFactory.createGameModelInstance(ModelDefinition.JEEP_TURRET_BASE),
            ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector3_1
            ),
            null
        )
        entityBuilder.addTurretComponent(player, true, true, 0.4F, cannon)
        entityBuilder.addTurretAutomationComponent()
        val turret = entityBuilder.finishAndAddToEngine()
        ComponentsMapper.turretBase.get(player).turret = turret
//        applyOpponentColor(turret, color, "tank_turret_texture")
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
                    gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SPARK_SMALL),
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
