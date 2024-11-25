package com.gadarts.returnfire.systems.map

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.GeneralUtils
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.MapInflater
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.components.AnimatedTextureComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.GroundBlastComponent
import com.gadarts.returnfire.components.GroundComponent
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.components.model.ModelInstanceComponent
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents

/**
 * Responsible to create the map from the loaded map file and manage general map procedures and ambient effects.
 */
class MapSystem : GameEntitySystem() {

    private val waterSplashEntitiesToRemove = com.badlogic.gdx.utils.Array<Entity>()
    private val ambSoundsHandler = AmbSoundsHandler()
    private var groundTextureAnimationStateTime = 0F
    private val animatedFloorsEntities: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(
            Family.all(
                GroundComponent::class.java,
                AnimatedTextureComponent::class.java
            ).get()
        )
    }
    private val waterSplashEntities: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(
            Family.all(
                GroundBlastComponent::class.java,
            ).get()
        )
    }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> =
        mapOf(SystemEvents.PHYSICS_DROWNING to object : HandlerOnEvent {
            override fun react(msg: Telegram, gameSessionData: GameSessionData, managers: Managers) {
                val position =
                    ComponentsMapper.modelInstance.get(msg.extraInfo as Entity).gameModelInstance.modelInstance.transform.getTranslation(
                        auxVector
                    )
                position.set(
                    position.x + MathUtils.randomSign() * MathUtils.random(0.2F),
                    0.05F,
                    position.z + MathUtils.randomSign() * MathUtils.random(0.2F)
                )
                managers.factories.specialEffectsFactory.generateWaterSplash(
                    position
                )
            }
        })

    override fun initialize(gameSessionData: GameSessionData, managers: Managers) {
        super.initialize(gameSessionData, managers)
        val tilesMapping = gameSessionData.currentMap.tilesMapping
        gameSessionData.tilesEntities =
            Array(tilesMapping.size) { arrayOfNulls(tilesMapping[0].size) }
        gameSessionData.floorModel = createFloorModel()
        gameSessionData.renderData.modelCache = ModelCache()
    }


    override fun onSystemReady() {
        super.onSystemReady()
        MapInflater(gameSessionData, managers, engine).inflate()
        engine.getEntitiesFor(Family.all(ModelInstanceComponent::class.java).get())
            .find { ComponentsMapper.modelInstance.get(it).gameModelInstance.definition == ModelDefinition.PIT }.let {
            Gdx.app.log("MapSystem", "Creating stage model instance.")
            val stageModelInstance = GameModelInstance(
                ModelInstance(managers.assetsManager.getAssetByDefinition(ModelDefinition.STAGE)),
                ModelDefinition.STAGE
            )
            EntityBuilder.begin().addModelInstanceComponent(
                stageModelInstance,
                ComponentsMapper.modelInstance.get(it).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector
                ).add(1F, -1F, 1F), null
            ).finishAndAddToEngine()
        }
    }


    override fun resume(delta: Long) {
        ambSoundsHandler.resume(delta)
    }

    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        ambSoundsHandler.update(managers)
        groundTextureAnimationStateTime += deltaTime
        for (entity in animatedFloorsEntities) {
            val keyFrame = ComponentsMapper.animatedTexture.get(entity).animation.getKeyFrame(
                groundTextureAnimationStateTime,
                true
            )
            (ComponentsMapper.modelInstance.get(entity).gameModelInstance.modelInstance.materials.get(
                0
            )
                .get(TextureAttribute.Diffuse) as TextureAttribute).textureDescription.texture =
                keyFrame
        }
        waterSplashEntitiesToRemove.clear()
        for (entity in waterSplashEntities) {
            val groundBlastComponent = ComponentsMapper.waterWave.get(entity)
            if (TimeUtils.timeSinceMillis(groundBlastComponent.creationTime) > groundBlastComponent.duration) {
                waterSplashEntitiesToRemove.add(entity)
            } else {
                val modelInstance =
                    ComponentsMapper.modelInstance.get(entity).gameModelInstance.modelInstance
                modelInstance.transform.scl(1 + groundBlastComponent.scalePace * deltaTime)
                val blendAttribute =
                    modelInstance.materials.get(0).get(BlendingAttribute.Type) as BlendingAttribute
                blendAttribute.opacity -= groundBlastComponent.fadeOutPace * deltaTime * 60F
            }
        }
        while (!waterSplashEntitiesToRemove.isEmpty) {
            val entity = waterSplashEntitiesToRemove.removeIndex(0)
            gameSessionData.groundBlastPool.free(ComponentsMapper.modelInstance.get(entity).gameModelInstance)
            engine.removeEntity(entity)
        }
    }

    override fun dispose() {
        gameSessionData.renderData.modelCache.dispose()
    }


    private fun createFloorModel(): Model {
        val builder = ModelBuilder()
        builder.begin()
        val texture =
            managers.assetsManager.getTexture("tile_water")
        GeneralUtils.createFlatMesh(builder, "floor", 0.5F, texture, 0F)
        return builder.end()
    }

    companion object {
        private val auxVector = Vector3()
    }
}
