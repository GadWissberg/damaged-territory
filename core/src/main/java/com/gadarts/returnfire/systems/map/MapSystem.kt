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
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.components.*
import com.gadarts.returnfire.components.cd.ChildDecal
import com.gadarts.returnfire.components.cd.DecalAnimation
import com.gadarts.returnfire.components.character.CharacterColor
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.factories.SpecialEffectsFactory
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.events.data.PhysicsCollisionEventData
import com.gadarts.returnfire.utils.GeneralUtils
import com.gadarts.returnfire.utils.MapInflater
import kotlin.math.max
import kotlin.math.min

class MapSystem(gamePlayManagers: GamePlayManagers) : GameEntitySystem(gamePlayManagers) {

    private val groundTextureAnimationHandler = GroundTextureAnimationHandler(gamePlayManagers.ecs.engine)
    private val landingMark: ChildDecal by lazy { createLandingMark() }
    private val waterSplashEntitiesToRemove = com.badlogic.gdx.utils.Array<Entity>()
    private val ambSoundsHandler = AmbSoundsHandler()
    private val waterSplashEntities: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(
            Family.all(
                GroundBlastComponent::class.java,
            ).get()
        )
    }

    private val bases: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(
            Family.all(
                BaseComponent::class.java,
            ).get()
        )
    }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> =
        mapOf(
            SystemEvents.PHYSICS_COLLISION to object : HandlerOnEvent {
                override fun react(
                    msg: Telegram,
                    gameSessionData: GameSessionData,
                    gamePlayManagers: GamePlayManagers
                ) {
                    val entity0 = PhysicsCollisionEventData.colObj0.userData as Entity
                    val entity1 = PhysicsCollisionEventData.colObj1.userData as Entity
                    handleCollisionRoadWithBullets(
                        entity0,
                        entity1
                    ) || handleCollisionRoadWithBullets(
                        entity1,
                        entity0
                    ) || handleCollisionRoadWithHeavyStuffOnHighSpeed(
                        entity0,
                        entity1
                    ) || handleCollisionRoadWithHeavyStuffOnHighSpeed(
                        entity1,
                        entity0
                    )
                }

            },
            SystemEvents.PHYSICS_DROWNING to object : HandlerOnEvent {
                override fun react(
                    msg: Telegram,
                    gameSessionData: GameSessionData,
                    gamePlayManagers: GamePlayManagers
                ) {
                    val entity = msg.extraInfo as Entity
                    val modelInstanceComponent = ComponentsMapper.modelInstance.get(entity) ?: return
                    if (entity.isRemoving || entity.isScheduledForRemoval) return

                    val position =
                        modelInstanceComponent.gameModelInstance.modelInstance.transform.getTranslation(
                            auxVector1
                        )
                    position.set(
                        position.x + MathUtils.randomSign() * MathUtils.random(0.2F),
                        SpecialEffectsFactory.WATER_SPLASH_Y,
                        position.z + MathUtils.randomSign() * MathUtils.random(0.2F)
                    )
                    gamePlayManagers.factories.specialEffectsFactory.generateWaterSplash(
                        position, ComponentsMapper.character.has(entity)
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
                override fun react(
                    msg: Telegram,
                    gameSessionData: GameSessionData,
                    gamePlayManagers: GamePlayManagers
                ) {
                    val base = findBase(msg.extraInfo as Entity)
                    closeDoors(base, gamePlayManagers)
                    landingMark.visible = false
                }
            },
            SystemEvents.CHARACTER_BOARDING to object : HandlerOnEvent {
                override fun react(
                    msg: Telegram,
                    gameSessionData: GameSessionData,
                    gamePlayManagers: GamePlayManagers
                ) {
                    val boardingComponent = ComponentsMapper.boarding.get(gameSessionData.gamePlayData.player)
                    if (boardingComponent.boardingAnimation == null && boardingComponent.isOnboarding()) {
                        val base = findBase(msg.extraInfo as Entity)
                        closeDoors(base, gamePlayManagers)
                    }
                }
            },
        )

    private fun handleCollisionRoadWithHeavyStuffOnHighSpeed(entity0: Entity, entity1: Entity): Boolean {
        val roadComponent = ComponentsMapper.road.get(entity0)
        val physicsComponent = ComponentsMapper.physics.get(entity1)
        if (roadComponent != null && physicsComponent != null) {
            val rigidBody = physicsComponent.rigidBody
            if (rigidBody.linearVelocity.len2() > 2 && rigidBody.mass > 5) {
                handleRoadHit(entity0, true)
            }
            return true
        }
        return false
    }

    private fun handleCollisionRoadWithBullets(entity0: Entity, entity1: Entity): Boolean {
        val roadComponent = ComponentsMapper.road.get(entity0)
        val bulletComponent = ComponentsMapper.bullet.get(entity1)
        if (roadComponent != null && bulletComponent != null) {
            if (bulletComponent.explosive) {
                handleRoadHit(entity0)
            }
            return true
        }
        return false
    }

    private fun handleRoadHit(
        roadEntity: Entity,
        forceDestruction: Boolean = false
    ) {
        val roadComponent = ComponentsMapper.road.get(roadEntity)
        if (roadComponent.hp > 0) {
            roadComponent.takeDamage()
            if (roadComponent.hp <= 0 || forceDestruction) {
                val modelInstance = ComponentsMapper.modelInstance.get(roadEntity).gameModelInstance.modelInstance
                val position =
                    modelInstance.transform.getTranslation(
                        auxVector1
                    )
                val assetsManager = gamePlayManagers.assetsManager
                gamePlayManagers.soundPlayer.play(
                    assetsManager.getAssetByDefinition(SoundDefinition.EXPLOSION_SMALL),
                    position
                )
                gamePlayManagers.factories.specialEffectsFactory.generateSmallFlyingParts(position.add(0F, 0.2F, 0F))
                val attribute = modelInstance.materials.get(0).get(TextureAttribute.Diffuse) as TextureAttribute
                val name = "${roadComponent.textureDefinition.fileName}_dead"
                gamePlayManagers.assetsManager.getTexturesDefinitions().definitions[name]?.let {
                    attribute.textureDescription.texture =
                        assetsManager.getTexture(it, MathUtils.random(1, it.frames) - 1)
                }
            }
        }
    }

    private fun findBase(entity: Entity): Entity {
        val characterColor = ComponentsMapper.character.get(entity).color
        return bases.find { ComponentsMapper.base.get(it).color == characterColor }!!
    }

    private fun closeDoors(base: Entity, gamePlayManagers: GamePlayManagers) {
        val baseComponent = ComponentsMapper.base.get(base)
        baseComponent.close()
        baseComponent.baseDoorSoundId =
            gamePlayManagers.soundPlayer.play(
                gamePlayManagers.assetsManager.getAssetByDefinition(SoundDefinition.BASE_DOOR_MOVE),
                ComponentsMapper.modelInstance.get(base).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector1
                )
            )
    }

    override fun initialize(gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        super.initialize(gameSessionData, gamePlayManagers)
        val tilesMapping = gameSessionData.mapData.currentMap.tilesMapping
        gameSessionData.mapData.tilesEntities =
            Array(tilesMapping.size) { arrayOfNulls(tilesMapping[0].size) }
        gameSessionData.renderData.floorModel = createFloorModel()
        gameSessionData.renderData.modelCache = ModelCache()
    }


    override fun onSystemReady() {
        super.onSystemReady()
        MapInflater(gameSessionData, gamePlayManagers, engine).inflate()
        bases.forEach {
            initializeBase(it)
        }
        gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.MAP_SYSTEM_READY.ordinal)
    }

    private fun initializeBase(base: Entity) {
        addStage(base)
        val baseComponent = ComponentsMapper.base.get(base)
        baseComponent.init(
            addBaseDoor(base, 0F, -1F),
            addBaseDoor(base, 180F, 1F)
        )
        val sourcePosition =
            ComponentsMapper.modelInstance.get(base).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector1
            )

        baseComponent.baseDoorSoundId =
            gamePlayManagers.soundPlayer.play(
                gamePlayManagers.assetsManager.getAssetByDefinition(SoundDefinition.BASE_DOOR_MOVE),
                sourcePosition
            )
    }

    private fun addBaseDoor(base: Entity, rotationAroundY: Float, relativeTargetX: Float): Entity {
        val doorModelInstance = GameModelInstance(
            ModelInstance(gamePlayManagers.assetsManager.getAssetByDefinition(ModelDefinition.PIT_DOOR)),
            ModelDefinition.PIT_DOOR
        )
        val basePosition =
            ComponentsMapper.modelInstance.get(base).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector1
            )
        val door = gamePlayManagers.ecs.entityBuilder.begin()
            .addModelInstanceComponent(
                doorModelInstance,
                basePosition.add(1F, -0.1F, 1F), null
            )
            .addBaseDoorComponent(basePosition.x, basePosition.x + relativeTargetX)
            .finishAndAddToEngine()
        doorModelInstance.modelInstance.transform.rotate(Vector3.Y, rotationAroundY)
        val baseComponent = ComponentsMapper.base.get(base)
        val color =
            if (baseComponent.color == CharacterColor.BROWN) "pit_door_texture_brown" else "pit_door_texture_green"
        val textureAttribute =
            ComponentsMapper.modelInstance.get(door).gameModelInstance.modelInstance.materials.get(0)
                .get(TextureAttribute.Diffuse) as TextureAttribute
        textureAttribute.textureDescription.texture =
            gamePlayManagers.assetsManager.getTexture(color)

        return door
    }

    private fun addStage(base: Entity): Entity {
        val color =
            if (ComponentsMapper.base.get(base).color == CharacterColor.BROWN) "stage_texture_brown" else "stage_texture_green"
        val texture =
            gamePlayManagers.assetsManager.getTexture(color)
        return gamePlayManagers.ecs.entityBuilder.begin()
            .addModelInstanceComponent(
                model = GameModelInstance(
                    ModelInstance(gamePlayManagers.assetsManager.getAssetByDefinition(ModelDefinition.STAGE)),
                    ModelDefinition.STAGE
                ),
                position = ComponentsMapper.modelInstance.get(base).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector1
                ).add(1F, StageComponent.BOTTOM_EDGE_Y, 1F),
                boundingBox = null,
                texture = texture
            )
            .addChildDecalComponent(
                listOf(landingMark), false
            )
            .addStageComponent(base)
            .finishAndAddToEngine()
    }

    private fun createLandingMark(): ChildDecal {
        val definition = gamePlayManagers.assetsManager.getTexturesDefinitions().definitions["landing_mark"]
        val landingMarkFrame0 = TextureRegion(gamePlayManagers.assetsManager.getTexture(definition!!, 0))
        val landingMarkFrame1 = TextureRegion(gamePlayManagers.assetsManager.getTexture(definition, 1))
        val decal = Decal.newDecal(2F, 2F, TextureRegion(landingMarkFrame0), true)
        decal.setColor(decal.color.r, decal.color.g, decal.color.b, 0.5F)
        val frames = com.badlogic.gdx.utils.Array<TextureRegion>()
        frames.add(landingMarkFrame0)
        frames.add(landingMarkFrame1)
        return ChildDecal(
            decal,
            Vector3(0F, 1F, 0F),
            Quaternion().setEulerAngles(0F, 90F, 0F),
            DecalAnimation(1F, frames)
        )

    }


    override fun resume(delta: Long) {
        ambSoundsHandler.resume(delta)
    }

    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        for (base in bases) {
            updateBaseDoors(base, deltaTime)
        }
        ambSoundsHandler.update(gamePlayManagers)
        groundTextureAnimationHandler.update(deltaTime)
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
            gameSessionData.gamePlayData.pools.groundBlastPool.free(ComponentsMapper.modelInstance.get(entity).gameModelInstance)
            engine.removeEntity(entity)
        }
    }

    private fun updateBaseDoors(base: Entity, deltaTime: Float) {
        val baseComponent = ComponentsMapper.base.get(base)
        if (baseComponent.isIdle()) return

        val westDoorTransform =
            ComponentsMapper.modelInstance.get(baseComponent.westDoor).gameModelInstance.modelInstance.transform
        val eastDoorTransform =
            ComponentsMapper.modelInstance.get(baseComponent.eastDoor).gameModelInstance.modelInstance.transform
        val stepSize = deltaTime * baseComponent.doorMoveState
        val westDoorBaseDoorComponent = ComponentsMapper.baseDoor.get(baseComponent.westDoor)
        val eastDoorBaseDoorComponent = ComponentsMapper.baseDoor.get(baseComponent.eastDoor)
        updateWestDoor(baseComponent, westDoorBaseDoorComponent, westDoorTransform, stepSize)
        updateEastDoor(baseComponent, eastDoorBaseDoorComponent, eastDoorTransform, stepSize)
    }

    private fun updateEastDoor(
        baseComponent: BaseComponent,
        eastDoorBaseDoorComponent: BaseDoorComponent,
        eastDoorTransform: Matrix4,
        stepSize: Float,
    ) {
        if (TimeUtils.timeSinceMillis(baseComponent.latestCloseTime) < DOORS_DELAY) return

        val eastDoorX = eastDoorTransform.getTranslation(auxVector2).x
        val isOpening = baseComponent.doorMoveState > 0
        val isClosing = baseComponent.doorMoveState < 0
        if ((isOpening && eastDoorX < eastDoorBaseDoorComponent.targetX)
            || (isClosing && eastDoorX > eastDoorBaseDoorComponent.initialX)
        ) {
            eastDoorTransform.trn(stepSize, 0F, 0F).getTranslation(auxVector1)
            auxVector1.x = max(eastDoorBaseDoorComponent.initialX, auxVector1.x)
            auxVector1.x = min(eastDoorBaseDoorComponent.targetX, auxVector1.x)
            eastDoorTransform.setTranslation(auxVector1)
        } else {
            baseComponent.setIdle()
            gamePlayManagers.soundPlayer.stop(
                gamePlayManagers.assetsManager.getAssetByDefinition(SoundDefinition.BASE_DOOR_MOVE),
                baseComponent.baseDoorSoundId
            )
            gamePlayManagers.soundPlayer.play(
                gamePlayManagers.assetsManager.getAssetByDefinition(SoundDefinition.BASE_DOOR_DONE),
                auxVector2
            )
        }
    }

    private fun updateWestDoor(
        baseComponent: BaseComponent,
        westDoorBaseDoorComponent: BaseDoorComponent,
        westDoorTransform: Matrix4,
        stepSize: Float
    ) {
        if (TimeUtils.timeSinceMillis(baseComponent.latestCloseTime) < DOORS_DELAY) return

        val isOpening = baseComponent.doorMoveState > 0
        val isClosing = baseComponent.doorMoveState < 0
        val westDoorX = westDoorTransform.getTranslation(auxVector1).x
        if ((isOpening && westDoorX > westDoorBaseDoorComponent.targetX)
            || (isClosing && westDoorX < westDoorBaseDoorComponent.initialX)
        ) {
            westDoorTransform.trn(-stepSize, 0F, 0F).getTranslation(auxVector1)
            auxVector1.x = max(westDoorBaseDoorComponent.targetX, auxVector1.x)
            auxVector1.x = min(westDoorBaseDoorComponent.initialX, auxVector1.x)
            westDoorTransform.setTranslation(auxVector1)
        }
    }


    override fun dispose() {
        gameSessionData.renderData.modelCache.dispose()
    }


    private fun createFloorModel(): Model {
        val builder = ModelBuilder()
        builder.begin()
        val texture =
            gamePlayManagers.assetsManager.getTexture("tile_water")
        GeneralUtils.createFlatMesh(builder, "floor", 0.5F, texture, 0F)
        return builder.end()
    }

    companion object {
        const val DOORS_DELAY = 1000F
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
    }
}
