package com.gadarts.returnfire.systems.map

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.GeneralUtils
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.MapInflater
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.components.*
import com.gadarts.returnfire.components.cd.ChildDecal
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.components.model.ModelInstanceComponent
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents

/**
 * Responsible to create the map from the loaded map file and manage general map procedures and ambient effects.
 */
class MapSystem(managers: Managers) : GameEntitySystem(managers) {

    private var doorMoveState: Int = 1
    private var baseDoorSoundId: Long = -1L
    private lateinit var eastDoor: Entity
    private lateinit var westDoor: Entity
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
        mapOf(
            SystemEvents.PHYSICS_DROWNING to object : HandlerOnEvent {
                override fun react(msg: Telegram, gameSessionData: GameSessionData, managers: Managers) {
                    val entity = msg.extraInfo as Entity
                    val modelInstanceComponent = ComponentsMapper.modelInstance.get(entity) ?: return
                    if (entity.isRemoving || entity.isScheduledForRemoval) return

                    val position =
                        modelInstanceComponent.gameModelInstance.modelInstance.transform.getTranslation(
                            auxVector1
                        )
                    position.set(
                        position.x + MathUtils.randomSign() * MathUtils.random(0.2F),
                        0.05F,
                        position.z + MathUtils.randomSign() * MathUtils.random(0.2F)
                    )
                    managers.factories.specialEffectsFactory.generateWaterSplash(
                        position
                    )
                    val physicsComponent = ComponentsMapper.physics.get(
                        entity
                    )
                    val rigidBody = physicsComponent.rigidBody
                    gameSessionData.physicsData.collisionWorld.removeRigidBody(
                        rigidBody
                    )
                    engine.removeEntity(entity)
                }
            },
            SystemEvents.CHARACTER_ONBOARDING_ANIMATION_DONE to object : HandlerOnEvent {
                override fun react(msg: Telegram, gameSessionData: GameSessionData, managers: Managers) {
                    closeDoors(managers)
                }
            },
            SystemEvents.CHARACTER_BOARDING to object : HandlerOnEvent {
                override fun react(msg: Telegram, gameSessionData: GameSessionData, managers: Managers) {
                    val boardingComponent = ComponentsMapper.boarding.get(gameSessionData.gameplayData.player)
                    if (boardingComponent.boardingAnimation == null && boardingComponent.isOnboarding()) {
                        closeDoors(managers)
                    }
                }
            })

    private fun closeDoors(managers: Managers) {
        doorMoveState = -1
        baseDoorSoundId =
            managers.soundPlayer.play(managers.assetsManager.getAssetByDefinition(SoundDefinition.BASE_DOOR_MOVE))
    }

    override fun initialize(gameSessionData: GameSessionData, managers: Managers) {
        super.initialize(gameSessionData, managers)
        val tilesMapping = gameSessionData.mapData.currentMap.tilesMapping
        gameSessionData.mapData.tilesEntities =
            Array(tilesMapping.size) { arrayOfNulls(tilesMapping[0].size) }
        gameSessionData.renderData.floorModel = createFloorModel()
        gameSessionData.renderData.modelCache = ModelCache()
    }


    override fun onSystemReady() {
        super.onSystemReady()
        MapInflater(gameSessionData, managers, engine).inflate()
        engine.getEntitiesFor(Family.all(ModelInstanceComponent::class.java).get())
            .find { ComponentsMapper.modelInstance.get(it).gameModelInstance.definition == ModelDefinition.PIT }.let {
                addStage(it!!)
                westDoor = addBaseDoor(it, 0F, -1F)
                eastDoor = addBaseDoor(it, 180F, 1F)
                baseDoorSoundId =
                    managers.soundPlayer.play(managers.assetsManager.getAssetByDefinition(SoundDefinition.BASE_DOOR_MOVE))
            }
    }

    private fun addBaseDoor(base: Entity, rotationAroundY: Float, relativeTargetX: Float): Entity {
        val doorModelInstance = GameModelInstance(
            ModelInstance(managers.assetsManager.getAssetByDefinition(ModelDefinition.PIT_DOOR)),
            ModelDefinition.PIT_DOOR
        )
        val basePosition =
            ComponentsMapper.modelInstance.get(base).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector1
            )
        val door = managers.entityBuilder.begin()
            .addModelInstanceComponent(
                doorModelInstance,
                basePosition.add(1F, -0.1F, 1F), null
            )
            .addBaseDoorComponent(basePosition.x, basePosition.x + relativeTargetX)
            .finishAndAddToEngine()
        doorModelInstance.modelInstance.transform.rotate(Vector3.Y, rotationAroundY)

        return door
    }

    private fun addStage(base: Entity): Entity {
        val stageModelInstance = GameModelInstance(
            ModelInstance(managers.assetsManager.getAssetByDefinition(ModelDefinition.STAGE)),
            ModelDefinition.STAGE
        )
        val decal = Decal.newDecal(2F, 2F, TextureRegion(managers.assetsManager.getTexture("landing_ok")), true)
        val color = decal.color
        decal.setColor(color.r, color.g, color.b, 0.5F)
        return managers.entityBuilder.begin()
            .addModelInstanceComponent(
                stageModelInstance,
                ComponentsMapper.modelInstance.get(base).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector1
                ).add(1F, StageComponent.BOTTOM_EDGE_Y, 1F), null
            )
            .addChildDecalComponent(
                listOf(
                    ChildDecal(
                        decal,
                        Vector3(0F, 1F, 0F),
                        Quaternion().setEulerAngles(0F, 90F, 0F)
                    )
                ), false
            )
            .addStageComponent()
            .finishAndAddToEngine()
    }


    override fun resume(delta: Long) {
        ambSoundsHandler.resume(delta)
    }

    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        updateBaseDoors(deltaTime)
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
            gameSessionData.pools.groundBlastPool.free(ComponentsMapper.modelInstance.get(entity).gameModelInstance)
            engine.removeEntity(entity)
        }
    }

    private fun updateBaseDoors(deltaTime: Float) {
        if (doorMoveState == 0) return

        val westDoorTransform = ComponentsMapper.modelInstance.get(westDoor).gameModelInstance.modelInstance.transform
        val eastDoorTransform = ComponentsMapper.modelInstance.get(eastDoor).gameModelInstance.modelInstance.transform
        val stepSize = deltaTime * 0.3F * doorMoveState
        val westDoorX = westDoorTransform.getTranslation(auxVector1).x
        val eastDoorX = eastDoorTransform.getTranslation(auxVector2).x
        val westDoorBaseDoorComponent = ComponentsMapper.baseDoor.get(westDoor)
        val eastDoorBaseDoorComponent = ComponentsMapper.baseDoor.get(eastDoor)
        val isOpening = doorMoveState > 0
        val isClosing = doorMoveState < 0
        if ((isOpening && westDoorX > westDoorBaseDoorComponent.targetX)
            || (isClosing && westDoorX < westDoorBaseDoorComponent.initialX)
        ) {
            westDoorTransform.trn(-stepSize, 0F, 0F)
        }
        if ((isOpening && eastDoorX < eastDoorBaseDoorComponent.targetX)
            || (isClosing && eastDoorX > eastDoorBaseDoorComponent.initialX)
        ) {
            eastDoorTransform.trn(stepSize, 0F, 0F)
        } else {
            doorMoveState = 0
            managers.soundPlayer.stop(
                managers.assetsManager.getAssetByDefinition(SoundDefinition.BASE_DOOR_MOVE),
                baseDoorSoundId
            )
            managers.soundPlayer.play(managers.assetsManager.getAssetByDefinition(SoundDefinition.BASE_DOOR_DONE))
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
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
    }
}
