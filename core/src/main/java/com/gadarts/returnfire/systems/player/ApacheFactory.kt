package com.gadarts.returnfire.systems.player

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.decals.Decal.newDecal
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.components.ArmComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.arm.ArmEffectsData
import com.gadarts.returnfire.components.arm.ArmProperties
import com.gadarts.returnfire.components.arm.ArmRenderData
import com.gadarts.returnfire.components.bullet.BulletBehavior
import com.gadarts.returnfire.components.cd.ChildDecal
import com.gadarts.returnfire.factories.GameModelInstanceFactory
import com.gadarts.returnfire.model.PlacedElement
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.data.GameSessionData.Companion.APACHE_SPARK_HEIGHT_BIAS
import com.gadarts.returnfire.systems.player.handlers.PlayerShootingHandler

class ApacheFactory(
    private val assetsManager: GameAssetManager,
    private val playerShootingHandler: PlayerShootingHandler,
    private val gameSessionData: GameSessionData,
    gameModelInstanceFactory: GameModelInstanceFactory
) :
    CharacterFactory(assetsManager, gameModelInstanceFactory) {
    override fun create(placedPlayer: PlacedElement): Entity {
        val machineGunSparkModel = assetsManager.getAssetByDefinition(ModelDefinition.MACHINE_GUN_SPARK)
        val secondarySpark = addSpark(machineGunSparkModel, secRelativePositionCalculator)
        val primarySpark =
            createPrimarySpark(ModelDefinition.MACHINE_GUN_SPARK, apachePrimaryRelativePositionCalculator)
        val entityBuilder = EntityBuilder.begin()
        addPlayerBaseComponents(entityBuilder, placedPlayer, primarySpark) {
            addApachePrimaryArmComponent(entityBuilder, primarySpark)
        }
        if (GameDebugSettings.DISPLAY_PROPELLER) {
            addPropeller(entityBuilder)
        }
        val propellerSound = assetsManager.getAssetByDefinition(SoundDefinition.PROPELLER)
        entityBuilder.addAmbSoundComponent(
            propellerSound
        )
        addSecondaryArmComponent(entityBuilder, secondarySpark)
        val player = entityBuilder.finish()
        ComponentsMapper.spark.get(secondarySpark).parent = player
        return player

    }

    private fun addApachePrimaryArmComponent(
        entityBuilder: EntityBuilder,
        primarySpark: Entity,
    ): EntityBuilder {
        entityBuilder.addPrimaryArmComponent(
            primarySpark,
            ArmProperties(
                1,
                assetsManager.getAssetByDefinition(SoundDefinition.MACHINE_GUN),
                APACHE_PRI_RELOAD_DUR,
                APACHE_PRI_BULLET_SPEED,
                ArmEffectsData(
                    null,
                    null,
                    gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE_SMALL),
                    null
                ),
                ArmRenderData(
                    ModelDefinition.BULLET,
                    assetsManager.getCachedBoundingBox(ModelDefinition.BULLET),
                    -45F,
                ),
                false,
                gameSessionData.pools.rigidBodyPools.obtainRigidBodyPool(ModelDefinition.BULLET),
            ),
            BulletBehavior.REGULAR
        )
        return entityBuilder
    }

    private val apachePrimaryRelativePositionCalculator = object : ArmComponent.RelativePositionCalculator {
        override fun calculate(parent: Entity, output: Vector3): Vector3 {
            val transform =
                ComponentsMapper.modelInstance.get(parent).gameModelInstance.modelInstance.transform
            val pos = output.set(0.3F, 0F, 0F).rot(transform)
            pos.y -= APACHE_SPARK_HEIGHT_BIAS
            return pos
        }
    }
    private val secRelativePositionCalculator = object : ArmComponent.RelativePositionCalculator {
        override fun calculate(parent: Entity, output: Vector3): Vector3 {
            playerShootingHandler.secondaryCreationSide =
                !playerShootingHandler.secondaryCreationSide
            val transform =
                ComponentsMapper.modelInstance.get(gameSessionData.player).gameModelInstance.modelInstance.transform
            val pos =
                output.set(
                    0.5F,
                    0F,
                    if (playerShootingHandler.secondaryCreationSide) 1F else -1F
                )
                    .rot(transform).scl(SECONDARY_POSITION_BIAS)
            pos.y -= APACHE_SPARK_HEIGHT_BIAS
            return pos
        }
    }

    private fun addSecondaryArmComponent(
        entityBuilder: EntityBuilder,
        secondarySpark: Entity,
    ): EntityBuilder {
        entityBuilder.addSecondaryArmComponent(
            secondarySpark,
            ArmProperties(
                10,
                assetsManager.getAssetByDefinition(SoundDefinition.MISSILE),
                SEC_RELOAD_DUR,
                SEC_BULLET_SPEED,
                ArmEffectsData(
                    ParticleEffectDefinition.EXPLOSION_SMALL,
                    gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE_EMIT),
                    gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SPARK_SMALL),
                    gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE_SMALL_LOOP),
                ),
                ArmRenderData(
                    ModelDefinition.MISSILE,
                    assetsManager.getCachedBoundingBox(ModelDefinition.MISSILE),
                    -5F
                ),
                true,
                gameSessionData.pools.rigidBodyPools.obtainRigidBodyPool(ModelDefinition.MISSILE),
            ),
            BulletBehavior.CURVE
        )
        return entityBuilder
    }

    private fun addPropeller(
        entityBuilder: EntityBuilder
    ) {
        val definitions = assetsManager.getTexturesDefinitions()
        val propTextureRegion =
            TextureRegion(assetsManager.getTexture(definitions.definitions["propeller_blurred"]!!))
        val propDec = newDecal(PROP_SIZE, PROP_SIZE, propTextureRegion, true)
        propDec.rotateX(90F)
        val decals = listOf(ChildDecal(propDec, Vector3.Zero))
        entityBuilder.addChildDecalComponent(decals)
    }

    companion object {
        private const val SEC_RELOAD_DUR = 2000L
        private const val PROP_SIZE = 1.5F
        private const val SEC_BULLET_SPEED = 5F
        private const val SECONDARY_POSITION_BIAS = 0.2F
        private const val APACHE_PRI_RELOAD_DUR = 125L
        private const val APACHE_PRI_BULLET_SPEED = 16F

    }
}