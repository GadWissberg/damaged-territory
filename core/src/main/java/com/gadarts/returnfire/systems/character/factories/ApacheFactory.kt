package com.gadarts.returnfire.systems.character.factories

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.decals.Decal
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
import com.gadarts.returnfire.components.cd.ChildDecal
import com.gadarts.returnfire.components.character.CharacterColor
import com.gadarts.returnfire.components.onboarding.ApacheBoardingAnimation
import com.gadarts.returnfire.factories.GameModelInstanceFactory
import com.gadarts.returnfire.managers.GameAssetManager
import com.gadarts.returnfire.model.PlacedElement
import com.gadarts.returnfire.model.definitions.SimpleCharacterDefinition
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.data.GameSessionData

class ApacheFactory(
    private val assetsManager: GameAssetManager,
    private val gameSessionData: GameSessionData,
    private val entityBuilder: EntityBuilder,
    gameModelInstanceFactory: GameModelInstanceFactory,
) :
    CharacterFactory(gameModelInstanceFactory, entityBuilder, assetsManager) {

    override fun create(base: PlacedElement, color: CharacterColor): Entity {
        val primarySpark =
            addSpark(
                assetsManager.getAssetByDefinition(ModelDefinition.MACHINE_GUN_SPARK),
                apachePrimaryRelativePositionCalculator
            )
        val secondarySpark = addSpark(
            assetsManager.getAssetByDefinition(ModelDefinition.MACHINE_GUN_SPARK),
            secRelativePositionCalculator
        )
        val entityBuilder = entityBuilder.begin()
        addCharacterBaseComponents(
            base,
            SimpleCharacterDefinition.APACHE,
            primarySpark,
            secondarySpark,
            {
                addApachePrimaryArmComponent(primarySpark)
            },
            {
                addSecondaryArmComponent(secondarySpark)
            },
            ApacheBoardingAnimation(entityBuilder),
            color
        )
        addPropeller()
        val character = entityBuilder.finish()
        ComponentsMapper.spark.get(secondarySpark).parent = character
        applyOpponentColor(character, color, "apache_texture")
        return character
    }

    override fun dispose() {
    }


    private fun addApachePrimaryArmComponent(
        primarySpark: Entity,
    ): EntityBuilder {
        val modelDefinition = ModelDefinition.CANNON_BULLET
        entityBuilder.addPrimaryArmComponent(
            primarySpark,
            ArmProperties(
                0.1F,
                assetsManager.getAssetByDefinition(SoundDefinition.MACHINE_GUN),
                APACHE_PRI_RELOAD_DUR,
                APACHE_PRI_BULLET_SPEED,
                ArmEffectsData(
                    null,
                    null,
                    gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SPARK_NO_SMOKE),
                    null
                ),
                ArmRenderData(
                    modelDefinition,
                    assetsManager.getCachedBoundingBox(modelDefinition),
                    -45F,
                ),
                false,
                gameSessionData.gamePlayData.pools.rigidBodyPools.obtainRigidBodyPool(modelDefinition),
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
            pos.y -= GameSessionData.APACHE_SPARK_HEIGHT_BIAS
            return pos
        }
    }

    private val secRelativePositionCalculator = object : ArmComponent.RelativePositionCalculator {
        override fun calculate(parent: Entity, output: Vector3): Vector3 {
            val secondaryArmComponent = ComponentsMapper.secondaryArm.get(parent)
            secondaryArmComponent.flipCreationSide()
            val transform =
                ComponentsMapper.modelInstance.get(gameSessionData.gamePlayData.player).gameModelInstance.modelInstance.transform
            val pos =
                output.set(
                    0.5F,
                    0F,
                    secondaryArmComponent.creationSide.toFloat()
                )
                    .rot(transform).scl(SECONDARY_POSITION_BIAS)
            pos.y -= GameSessionData.APACHE_SPARK_HEIGHT_BIAS
            return pos
        }
    }

    private fun addSecondaryArmComponent(
        secondarySpark: Entity,
    ): EntityBuilder {
        entityBuilder.addSecondaryArmComponent(
            secondarySpark,
            ArmProperties(
                12F,
                assetsManager.getAssetByDefinition(SoundDefinition.MISSILE),
                SEC_RELOAD_DUR,
                SEC_BULLET_SPEED,
                ArmEffectsData(
                    ParticleEffectDefinition.EXPLOSION_MED,
                    gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE_EMIT),
                    gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SPARK_SMALL),
                    gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE_SMALL_LOOP),
                ),
                ArmRenderData(
                    ModelDefinition.MISSILE,
                    assetsManager.getCachedBoundingBox(ModelDefinition.MISSILE),
                    -5F
                ),
                true,
                gameSessionData.gamePlayData.pools.rigidBodyPools.obtainRigidBodyPool(ModelDefinition.MISSILE),
            ),
            BulletBehavior.CURVE
        )
        return entityBuilder
    }

    private fun addPropeller(
    ) {
        val definitions = assetsManager.getTexturesDefinitions()
        val propTextureRegion =
            TextureRegion(assetsManager.getTexture(definitions.definitions["propeller_blurred"]!!))
        val propDec = Decal.newDecal(PROP_SIZE, PROP_SIZE, propTextureRegion, true)
        propDec.rotateX(90F)
        propDec.setColor(propDec.color.r, propDec.color.g, propDec.color.b, 0F)
        val childDecal = ChildDecal(propDec, Vector3(0F, 0.09F, 0F), null)
        val decals = listOf(childDecal)
        entityBuilder.addChildDecalComponent(decals)
        val propellerGameModelInstance = gameModelInstanceFactory.createGameModelInstance(ModelDefinition.PROPELLER)
        propellerGameModelInstance.modelInstance.materials.get(0).set(BlendingAttribute())
        entityBuilder.addChildModelInstanceComponent(
            propellerGameModelInstance,
            false
        )
    }

    companion object {
        private const val SEC_RELOAD_DUR = 2000L
        private const val PROP_SIZE = 1.5F
        private const val SEC_BULLET_SPEED = 7F
        private const val SECONDARY_POSITION_BIAS = 0.2F
        private const val APACHE_PRI_RELOAD_DUR = 125L
        private const val APACHE_PRI_BULLET_SPEED = 32F
    }
}
