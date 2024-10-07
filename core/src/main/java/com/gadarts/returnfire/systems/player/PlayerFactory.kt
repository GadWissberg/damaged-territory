package com.gadarts.returnfire.systems.player

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
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
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.model.CharacterDefinition
import com.gadarts.returnfire.model.PlacedElement
import com.gadarts.returnfire.model.SimpleCharacterDefinition
import com.gadarts.returnfire.model.TurretCharacterDefinition
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.data.GameSessionData.Companion.APACHE_SPARK_HEIGHT_BIAS
import com.gadarts.returnfire.systems.player.handlers.PlayerShootingHandler

class PlayerFactory(
    private val assetsManager: GameAssetManager,
    private val gameSessionData: GameSessionData,
    playerShootingHandler: PlayerShootingHandler
) {
    fun create(placedPlayer: PlacedElement): Entity {
        var player: Entity? = null
        if (placedPlayer.definition == SimpleCharacterDefinition.APACHE) {
            player = createApache(placedPlayer)
        } else if (placedPlayer.definition == TurretCharacterDefinition.TANK) {
            player = createTank(placedPlayer)
        }
        return player!!
    }

    private fun createTank(placedPlayer: PlacedElement): Entity {
        val primarySpark = createPrimarySpark()
        val entityBuilder = EntityBuilder.begin()
        addPlayerBaseComponents(entityBuilder, placedPlayer, primarySpark)
        entityBuilder.addTurretBaseComponent()
        val engineSound = assetsManager.getAssetByDefinition(SoundDefinition.ENGINE)
        entityBuilder.addAmbSoundComponent(
            engineSound
        )
        val player = entityBuilder.finish()
        val cannon = addTankCannon(entityBuilder, player)
        EntityBuilder.begin()
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
        val turret = entityBuilder.finishAndAddToEngine()
        ComponentsMapper.turretBase.get(player).turret = turret
        return player
    }

    private fun addTankCannon(
        entityBuilder: EntityBuilder,
        player: Entity
    ): Entity {
        EntityBuilder.begin()
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

    private fun createApache(
        placedPlayer: PlacedElement,
    ): Entity {
        val machineGunSparkModel = assetsManager.getAssetByDefinition(ModelDefinition.MACHINE_GUN_SPARK)
        val secondarySpark = addSpark(machineGunSparkModel, secRelativePositionCalculator)
        val primarySpark = createPrimarySpark()
        val entityBuilder = EntityBuilder.begin()
        addPlayerBaseComponents(entityBuilder, placedPlayer, primarySpark)
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

    private fun addPlayerBaseComponents(
        entityBuilder: EntityBuilder,
        placedPlayer: PlacedElement,
        primarySpark: Entity
    ) {
        val definition = placedPlayer.definition as CharacterDefinition
        entityBuilder.addModelInstanceComponent(
            createPlayerModelInstance(),
            auxVector3_1.set(placedPlayer.col.toFloat(), definition.getStartHeight(), placedPlayer.row.toFloat()),
            null,
        )
        entityBuilder.addCharacterComponent(GameDebugSettings.SELECTED_VEHICLE)
        entityBuilder.addPlayerComponent()
        if (GameDebugSettings.SELECTED_VEHICLE == SimpleCharacterDefinition.APACHE) {
            addApachePrimaryArmComponent(entityBuilder, primarySpark)
        } else if (GameDebugSettings.SELECTED_VEHICLE == TurretCharacterDefinition.TANK) {
            addTankPrimaryArmComponent(entityBuilder, primarySpark)
        }
        ComponentsMapper.spark.get(primarySpark).parent = EntityBuilder.entity!!
    }

    private fun createPrimarySpark(): Entity {
        val sparkModel: ModelDefinition
        val calculator =
            if (GameDebugSettings.SELECTED_VEHICLE == SimpleCharacterDefinition.APACHE) {
                sparkModel = ModelDefinition.MACHINE_GUN_SPARK
                apachePrimaryRelativePositionCalculator
            } else {
                sparkModel = ModelDefinition.CANNON_SPARK
                tankPrimaryRelativePositionCalculator
            }
        val primarySpark = addSpark(assetsManager.getAssetByDefinition(sparkModel), calculator)
        return primarySpark
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
                    gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE_MED),
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

    private fun addSpark(
        machineGunSparkModel: Model,
        relativePositionCalculator: ArmComponent.RelativePositionCalculator
    ): Entity {
        return EntityBuilder.begin()
            .addModelInstanceComponent(
                GameModelInstance(ModelInstance(machineGunSparkModel), ModelDefinition.MACHINE_GUN_SPARK),
                Vector3(),
                null,
                hidden = true
            )
            .addSparkComponent(relativePositionCalculator)
            .finishAndAddToEngine()
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
        entityBuilder.addChildDecalComponent(decals, true)
    }


    private fun createPlayerModelInstance(): GameModelInstance {
        val modelDefinition = GameDebugSettings.SELECTED_VEHICLE.getModelDefinition()
        val model = assetsManager.getAssetByDefinition(modelDefinition)
        return GameModelInstance(
            ModelInstance(model),
            modelDefinition,
        )
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
        private val auxVector3_1 = Vector3()
        private const val APACHE_PRI_RELOAD_DUR = 125L
        private const val TANK_PRI_RELOAD_DUR = 2000L
        private const val SEC_RELOAD_DUR = 2000L
        private const val PROP_SIZE = 1.5F
        private const val APACHE_PRI_BULLET_SPEED = 16F
        private const val TANK_PRI_BULLET_SPEED = 8F
        private const val SEC_BULLET_SPEED = 5F
        private const val SECONDARY_POSITION_BIAS = 0.2F
    }

}
