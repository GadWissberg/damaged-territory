package com.gadarts.returnfire.ecs.systems.map

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject.CollisionFlags
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.components.ElevatorComponent
import com.gadarts.returnfire.ecs.components.FlagComponent
import com.gadarts.returnfire.ecs.components.FlagFloorComponent
import com.gadarts.returnfire.ecs.components.cd.ChildDecal
import com.gadarts.returnfire.ecs.components.cd.DecalAnimation
import com.gadarts.returnfire.ecs.components.model.GameModelInstance
import com.gadarts.returnfire.ecs.components.pit.ElevatorDoorComponent
import com.gadarts.returnfire.ecs.components.pit.HangarComponent
import com.gadarts.returnfire.ecs.systems.GameEntitySystem
import com.gadarts.returnfire.ecs.systems.HandlerOnEvent
import com.gadarts.returnfire.ecs.systems.data.GameSessionData
import com.gadarts.returnfire.ecs.systems.events.SystemEvents
import com.gadarts.returnfire.ecs.systems.map.handlers.amb.AmbEffectsHandlers
import com.gadarts.returnfire.ecs.systems.map.react.*
import com.gadarts.returnfire.factories.SpecialEffectsFactory
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.model.MapGraphType
import com.gadarts.returnfire.utils.MapInflaterImpl
import com.gadarts.returnfire.utils.MapUtils
import com.gadarts.returnfire.utils.ModelUtils
import com.gadarts.shared.SharedUtils
import com.gadarts.shared.SharedUtils.DROWNING_HEIGHT
import com.gadarts.shared.assets.definitions.ParticleEffectDefinition.FIRE_LOOP_SMALL
import com.gadarts.shared.assets.definitions.ParticleEffectDefinition.SMOKE_UP_LOOP
import com.gadarts.shared.assets.definitions.SoundDefinition
import com.gadarts.shared.assets.definitions.model.ModelDefinition
import com.gadarts.shared.data.CharacterColor
import com.gadarts.shared.data.ImmutableGameModelInstanceInfo
import kotlin.math.max
import kotlin.math.min

class MapSystemImpl(gamePlayManagers: GamePlayManagers) : GameEntitySystem(gamePlayManagers), MapSystem {

    private val mapSystemRelatedEntities = MapSystemRelatedEntities(gamePlayManagers.ecs.engine)
    private val ambEffectsHandlers = AmbEffectsHandlers(gamePlayManagers, this, mapSystemRelatedEntities)
    private val landingMark: ChildDecal by lazy { createLandingMark() }

    private val hangars: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(
            Family.all(
                HangarComponent::class.java,
            ).get()
        )
    }
    private val flags: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(
            Family.all(
                FlagComponent::class.java,
            ).get()
        )
    }


    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> =
        mapOf(
            SystemEvents.PHYSICS_COLLISION to MapSystemOnPhysicsCollision(this),
            SystemEvents.PHYSICS_DROWNED to object : HandlerOnEvent {
                override fun react(
                    msg: Telegram,
                    gameSessionData: GameSessionData,
                    gamePlayManagers: GamePlayManagers
                ) {
                    val entity = msg.extraInfo as Entity
                    if (ComponentsMapper.amb.has(entity)) {
                        gamePlayManagers.dispatcher.dispatchMessage(
                            SystemEvents.REMOVE_ENTITY.ordinal,
                            entity
                        )
                    }
                }
            },
            SystemEvents.CHARACTER_ONBOARDING_ANIMATION_DONE to MapSystemOnCharacterOnboardingAnimationDone(this),
            SystemEvents.CHARACTER_ONBOARDING_BEGIN to MapSystemOnCharacterBoarding(this),
            SystemEvents.EXPLOSION_PUSH_BACK to MapSystemOnExplosionPushBack(mapSystemRelatedEntities),
            SystemEvents.CHARACTER_DEATH_SEQUENCE_FINISHED to object : HandlerOnEvent {
                override fun react(
                    msg: Telegram,
                    gameSessionData: GameSessionData,
                    gamePlayManagers: GamePlayManagers
                ) {
                    val entity = msg.extraInfo as Entity
                    if (!ComponentsMapper.amb.has(entity)) return

                    ambEffectsHandlers.fallingBuildingsHandler.collapseBuilding(
                        entity,
                        gameSessionData,
                        gamePlayManagers
                    )
                }
            },
            SystemEvents.CHARACTER_DIED to MapSystemOnCharacterDied(),
            SystemEvents.OPPONENT_ENTERED_GAME_PLAY_SCREEN to MapSystemOnOpponentEnteredGamePlayScreen(this)
        )


    object TreeExplosionPushBackEffect : ExplosionPushBackEffect {
        override fun go(entity: Entity, affectedEntityPosition: Vector3, explosionPosition: Vector3) {
            ComponentsMapper.ambAnimation.get(entity)?.applyAffectedByExplosionAnimation()
        }

    }

    object FlyingPartExplosionPushBackEffect : ExplosionPushBackEffect {
        override fun go(entity: Entity, affectedEntityPosition: Vector3, explosionPosition: Vector3) {
            ComponentsMapper.physics.get(entity).rigidBody.applyCentralImpulse(
                auxVector3.set(affectedEntityPosition).sub(explosionPosition).nor()
                    .scl(MathUtils.random(0.5F, 1F)),
            )
        }

    }

    interface ExplosionPushBackEffect {
        fun go(entity: Entity, affectedEntityPosition: Vector3, explosionPosition: Vector3)

    }


    override fun destroyTree(
        tree: Entity,
        decorateWithSmokeAndFire: Boolean
    ) {
        val position =
            ComponentsMapper.modelInstance.get(tree).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector4
            )
        blowAmbToParts(
            auxVector3.set(position),
            ModelDefinition.PALM_TREE_LEAF,
            1,
            5,
            0.9F,
            decorateWithSmokeAndFire,
            0.125F,
            0.21F,
            AMB_PART_CREATION_POSITION_BIAS
        )
        blowAmbToParts(
            auxVector3.set(position),
            ModelDefinition.PALM_TREE_PART,
            1,
            2,
            1F,
            decorateWithSmokeAndFire,
            0.125F,
            0.25F,
            AMB_PART_CREATION_POSITION_BIAS
        )
        destroyAmbObject(tree)
        gamePlayManagers.soundManager.play(
            gamePlayManagers.assetsManager.getAssetByDefinition(SoundDefinition.TREE_FALL),
            position
        )
    }

    override fun destroyAmbObject(
        amb: Entity
    ) {
        val position =
            ComponentsMapper.modelInstance.get(amb).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector1
            )
        val ambComponent = ComponentsMapper.amb.get(amb)
        gamePlayManagers.stainsHandler.addBigHole(position)
        ambComponent.hp = 0
        if (ComponentsMapper.fence.has(amb)) {
            val fenceComponent = ComponentsMapper.fence.get(amb)
            val entityBuilder = gamePlayManagers.ecs.entityBuilder
            val gameModelInstanceFactory = gamePlayManagers.factories.gameModelInstanceFactory
            fenceComponent.left?.let {
                entityBuilder.addChildModelInstanceComponentToEntity(
                    it,
                    gameModelInstanceFactory.createGameModelInstance(ModelDefinition.FENCE_DESTROYED_RIGHT),
                    true,
                    auxVector1.set(0F, 0F, 0.95F)
                )
            }
            fenceComponent.right?.let {
                entityBuilder.addChildModelInstanceComponentToEntity(
                    it,
                    gameModelInstanceFactory.createGameModelInstance(ModelDefinition.FENCE_DESTROYED_LEFT),
                    true,
                    auxVector1.set(0F, 0F, -0.95F)
                )
            }
        }
        engine.removeEntity(amb)
    }

    override fun blowAmbToParts(
        position: Vector3,
        modelDefinition: ModelDefinition,
        min: Int,
        max: Int,
        gravityScale: Float,
        decorateWithSmokeAndFire: Boolean,
        minImpulse: Float,
        maxImpulse: Float,
        positionBiasMax: Float
    ) {
        val numberOfParts = MathUtils.random(min, max)
        for (i in 0 until numberOfParts) {
            val entity = createAmbPartEntity(modelDefinition, position, positionBiasMax)
            if (decorateWithSmokeAndFire && MathUtils.random() > 0.6F) {
                gamePlayManagers.ecs.entityBuilder.addParticleEffectComponentToEntity(
                    entity,
                    gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(
                        if (MathUtils.randomBoolean()) FIRE_LOOP_SMALL else SMOKE_UP_LOOP
                    ), ttlInSeconds = MathUtils.random(10, 20), ttlForComponentOnly = true
                )
            }
            addPhysicsToAmbPart(
                entity,
                gravityScale,
                minImpulse,
                maxImpulse,
            )
        }
    }

    private fun createAmbPartEntity(
        modelDefinition: ModelDefinition,
        position: Vector3,
        positionBiasMax: Float
    ): Entity {
        val gameModelInstance = gamePlayManagers.factories.gameModelInstanceFactory.createGameModelInstance(
            modelDefinition,
        )
        val entity = gamePlayManagers.ecs.entityBuilder.begin()
            .addDrowningEffectComponent()
            .addModelInstanceComponent(
                model = gameModelInstance,
                position = auxVector1.set(position).add(
                    MathUtils.random(-positionBiasMax, positionBiasMax),
                    MathUtils.random(0.1F, positionBiasMax),
                    MathUtils.random(-positionBiasMax, positionBiasMax)
                ),
                boundingBox = gamePlayManagers.assetsManager.getCachedBoundingBox(modelDefinition),
            )
            .addFlyingPartComponent()
            .finishAndAddToEngine()
        ambEffectsHandlers.fadingAwayHandler.add(entity)
        gameModelInstance.modelInstance.transform.rotate(
            auxQuat.idt().setEulerAngles(
                MathUtils.random(0F, 360F),
                MathUtils.random(0F, 360F),
                MathUtils.random(0F, 360F)
            )
        )
        return entity
    }

    private fun addPhysicsToAmbPart(
        entity: Entity,
        gravityScale: Float,
        minImpulse: Float,
        maxImpulse: Float,
    ) {
        val gameModelInstance = ComponentsMapper.modelInstance.get(entity).gameModelInstance
        gamePlayManagers.ecs.entityBuilder.addPhysicsComponentPooledToEntity(
            entity,
            gameSessionData.gamePlayData.pools.rigidBodyPools.obtainRigidBodyPool(gameModelInstance.gameModelInstanceInfo?.modelDefinition!!),
            CollisionFlags.CF_CHARACTER_OBJECT,
            gameModelInstance.modelInstance.transform,
            gravityScale
        ).rigidBody.applyImpulse(
            auxVector1.set(
                MathUtils.random(-1F, 1F),
                MathUtils.random(-1F, 1F),
                MathUtils.random(-1F, 1F)
            ).scl(MathUtils.random(minImpulse, maxImpulse)),
            auxVector2.set(
                MathUtils.random(-AMB_PART_IMPULSE_COMPONENT, AMB_PART_IMPULSE_COMPONENT),
                MathUtils.random(-AMB_PART_IMPULSE_COMPONENT, AMB_PART_IMPULSE_COMPONENT),
                MathUtils.random(-AMB_PART_IMPULSE_COMPONENT, AMB_PART_IMPULSE_COMPONENT)
            )
        )
    }


    override fun findHangar(entity: Entity): Entity {
        val characterColor = ComponentsMapper.character.get(entity).color
        return hangars.find { ComponentsMapper.hangar.get(it).color == characterColor }!!
    }

    override fun closeDoors(base: Entity) {
        val baseComponent = ComponentsMapper.hangar.get(base)
        baseComponent.close()
        baseComponent.baseDoorSoundId =
            gamePlayManagers.soundManager.play(
                gamePlayManagers.assetsManager.getAssetByDefinition(SoundDefinition.BASE_DOOR_MOVE),
                ComponentsMapper.modelInstance.get(base).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector1
                )
            )
    }

    override fun hideLandingMark() {
        landingMark.visible = false
    }

    override fun initialize(gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        super.initialize(gameSessionData, gamePlayManagers)
        ambEffectsHandlers.waterSplashHandler.init(gameSessionData)
        gameSessionData.renderData.floorModel = createFloorModel()
        engine.addEntityListener(object : EntityListener {
            override fun entityAdded(entity: Entity) {

            }

            override fun entityRemoved(entity: Entity) {
                if (MapUtils.isEntityMarksNodeAsBlocked(entity)) {
                    val position = ComponentsMapper.modelInstance.get(entity).gameModelInstance.modelInstance.transform
                        .getTranslation(auxVector1)
                    gameSessionData.mapData.mapGraph.getNode(position.x.toInt(), position.z.toInt())
                        .let { node ->
                            node.type = MapGraphType.AVAILABLE
                        }
                }
            }

        })
    }


    override fun onSystemReady() {
        super.onSystemReady()
        MapInflaterImpl(gameSessionData, gamePlayManagers, engine).inflate()
        hangars.forEach {
            initializeHangar(it)
        }
        val flagFloors = engine.getEntitiesFor(Family.all(FlagFloorComponent::class.java).get())
        flags.forEach {
            val flagComponent = ComponentsMapper.flag.get(it)
            val flagFloor =
                flagFloors.first { floor -> ComponentsMapper.flagFloor.get(floor).color == flagComponent.color }
            flagComponent.follow = flagFloor
        }
        gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.MAP_SYSTEM_READY.ordinal)
    }

    private fun initializeHangar(hangar: Entity) {
        addElevator(hangar)
        val baseComponent = ComponentsMapper.hangar.get(hangar)
        baseComponent.init(
            addBaseDoor(hangar, 0F, -1F),
            addBaseDoor(hangar, 180F, 1F)
        )
        val sourcePosition =
            ComponentsMapper.modelInstance.get(hangar).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector1
            )

        baseComponent.baseDoorSoundId =
            gamePlayManagers.soundManager.play(
                gamePlayManagers.assetsManager.getAssetByDefinition(SoundDefinition.BASE_DOOR_MOVE),
                sourcePosition
            )
    }

    private fun addBaseDoor(base: Entity, rotationAroundY: Float, relativeTargetX: Float): Entity {
        val doorModelInstance = GameModelInstance(
            ModelInstance(gamePlayManagers.assetsManager.getAssetByDefinition(ModelDefinition.PIT_DOOR)),
            ImmutableGameModelInstanceInfo(ModelDefinition.PIT_DOOR),
        )
        val basePosition =
            ComponentsMapper.modelInstance.get(base).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector1
            )
        val door = gamePlayManagers.ecs.entityBuilder.begin()
            .addModelInstanceComponent(
                doorModelInstance,
                basePosition.add(1F, -0.2F, 1F), null
            )
            .addBaseDoorComponent(basePosition.x, basePosition.x + relativeTargetX)
            .finishAndAddToEngine()
        doorModelInstance.modelInstance.transform.rotate(Vector3.Y, rotationAroundY)
        val baseComponent = ComponentsMapper.hangar.get(base)
        val color =
            if (baseComponent.color == CharacterColor.BROWN) "pit_door_texture_brown" else "pit_door_texture_green"
        val textureAttribute =
            ComponentsMapper.modelInstance.get(door).gameModelInstance.modelInstance.materials.get(0)
                .get(TextureAttribute.Diffuse) as TextureAttribute
        textureAttribute.textureDescription.texture =
            gamePlayManagers.assetsManager.getTexture(color)

        return door
    }

    private fun addElevator(hangar: Entity): Entity {
        val color =
            if (ComponentsMapper.hangar.get(hangar).color == CharacterColor.BROWN) "stage_texture_brown" else "stage_texture_green"
        val texture =
            gamePlayManagers.assetsManager.getTexture(color)
        return gamePlayManagers.ecs.entityBuilder.begin()
            .addModelInstanceComponent(
                model = GameModelInstance(
                    ModelInstance(gamePlayManagers.assetsManager.getAssetByDefinition(ModelDefinition.STAGE)),
                    ImmutableGameModelInstanceInfo(ModelDefinition.STAGE),
                ),
                position = ModelUtils.getPositionOfModel(hangar).add(1F, ElevatorComponent.BOTTOM_EDGE_Y, 1F),
                boundingBox = null,
                texture = texture
            )
            .addChildDecalComponent(
                listOf(landingMark), false
            )
            .addElevatorComponent(hangar)
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
        ambEffectsHandlers.ambSoundsHandler.resume(delta)
    }

    override fun update(deltaTime: Float) {
        if (isGamePaused()) return

        for (hangar in hangars) {
            updateElevatorDoors(hangar, deltaTime)
        }
        ambEffectsHandlers.ambSoundsHandler.update(gamePlayManagers)
        ambEffectsHandlers.groundTextureAnimationHandler.update(deltaTime)
        ambEffectsHandlers.waterSplashHandler.update(deltaTime)
        ambEffectsHandlers.fadingAwayHandler.update(deltaTime)
        ambEffectsHandlers.treeEffectsHandler.update()
        ambEffectsHandlers.fallingBuildingsHandler.updateFallingBuildings()
        updateDrowns()
        for (flag in flags) {
            val follow = ComponentsMapper.flag.get(flag).follow
            if (follow != null) {
                val followPosition = ModelUtils.getPositionOfModel(follow)
                ComponentsMapper.modelInstance.get(flag).gameModelInstance.modelInstance.transform.setTranslation(
                    auxVector1.set(followPosition).add(0F, 0.25F, 0F)
                )
            }
        }
    }

    private fun updateDrowns() {
        for (entity in mapSystemRelatedEntities.drownableEntities) {
            val position = ComponentsMapper.modelInstance.get(entity).gameModelInstance.modelInstance.transform
                .getTranslation(auxVector1)
            val drowningHeight =
                if (ComponentsMapper.amb.has(entity)) ComponentsMapper.amb.get(entity).def.drowningHeight else DROWNING_HEIGHT
            if (position.y < drowningHeight) {
                gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.PHYSICS_DROWNED.ordinal, entity)
            } else if (position.y < -0.1F) {
                val drowningComponent = ComponentsMapper.drowningEffect.get(entity)
                val x = position.x.toInt()
                val z = position.z.toInt()
                val groundBitMap = gameSessionData.mapData.groundBitMap
                val currentMap = gameSessionData.mapData.loadedMap
                if (x >= 0 && z >= 0
                    && x < currentMap.width && z < currentMap.depth
                    && drowningComponent != null
                    && TimeUtils.timeSinceMillis(drowningComponent.lastSplashTime) > 500
                ) {
                    val isWater = groundBitMap[z][x] == 0
                    if (isWater) {
                        drowningComponent.refreshLastSplashTime()
                        position.set(
                            position.x,
                            SpecialEffectsFactory.WATER_SPLASH_Y,
                            position.z
                        )
                        gamePlayManagers.factories.specialEffectsFactory.generateWaterSplash(
                            position, ComponentsMapper.character.has(entity)
                        )
                    }
                }
            }
        }
    }


    private fun updateElevatorDoors(hangar: Entity, deltaTime: Float) {
        val hangarComponent = ComponentsMapper.hangar.get(hangar)
        if (hangarComponent.isIdle()) return

        val westDoorTransform =
            ComponentsMapper.modelInstance.get(hangarComponent.westDoor).gameModelInstance.modelInstance.transform
        val eastDoorTransform =
            ComponentsMapper.modelInstance.get(hangarComponent.eastDoor).gameModelInstance.modelInstance.transform
        val stepSize = deltaTime * hangarComponent.doorMoveState
        val westDoorBaseDoorComponent = ComponentsMapper.elevatorDoor.get(hangarComponent.westDoor)
        val eastDoorBaseDoorComponent = ComponentsMapper.elevatorDoor.get(hangarComponent.eastDoor)
        updateWestDoor(hangarComponent, westDoorBaseDoorComponent, westDoorTransform, stepSize)
        updateEastDoor(hangarComponent, eastDoorBaseDoorComponent, eastDoorTransform, stepSize)
    }

    private fun updateEastDoor(
        hangarComponent: HangarComponent,
        eastDoorElevatorDoorComponent: ElevatorDoorComponent,
        eastDoorTransform: Matrix4,
        stepSize: Float,
    ) {
        if (TimeUtils.timeSinceMillis(hangarComponent.latestCloseTime) < DOORS_DELAY) return

        val eastDoorX = eastDoorTransform.getTranslation(auxVector2).x
        val isOpening = hangarComponent.doorMoveState > 0
        val isClosing = hangarComponent.doorMoveState < 0
        if ((isOpening && eastDoorX < eastDoorElevatorDoorComponent.targetX)
            || (isClosing && eastDoorX > eastDoorElevatorDoorComponent.initialX)
        ) {
            eastDoorTransform.trn(stepSize, 0F, 0F).getTranslation(auxVector1)
            auxVector1.x = max(eastDoorElevatorDoorComponent.initialX, auxVector1.x)
            auxVector1.x = min(eastDoorElevatorDoorComponent.targetX, auxVector1.x)
            eastDoorTransform.setTranslation(auxVector1)
        } else {
            hangarComponent.setIdle()
            gamePlayManagers.soundManager.stop(
                gamePlayManagers.assetsManager.getAssetByDefinition(SoundDefinition.BASE_DOOR_MOVE),
                hangarComponent.baseDoorSoundId
            )
            gamePlayManagers.soundManager.play(
                gamePlayManagers.assetsManager.getAssetByDefinition(SoundDefinition.BASE_DOOR_DONE),
                auxVector2
            )
        }
    }

    private fun updateWestDoor(
        hangarComponent: HangarComponent,
        westDoorElevatorDoorComponent: ElevatorDoorComponent,
        westDoorTransform: Matrix4,
        stepSize: Float
    ) {
        if (TimeUtils.timeSinceMillis(hangarComponent.latestCloseTime) < DOORS_DELAY) return

        val isOpening = hangarComponent.doorMoveState > 0
        val isClosing = hangarComponent.doorMoveState < 0
        val westDoorX = westDoorTransform.getTranslation(auxVector1).x
        if ((isOpening && westDoorX > westDoorElevatorDoorComponent.targetX)
            || (isClosing && westDoorX < westDoorElevatorDoorComponent.initialX)
        ) {
            westDoorTransform.trn(-stepSize, 0F, 0F).getTranslation(auxVector1)
            auxVector1.x = max(westDoorElevatorDoorComponent.targetX, auxVector1.x)
            auxVector1.x = min(westDoorElevatorDoorComponent.initialX, auxVector1.x)
            westDoorTransform.setTranslation(auxVector1)
        }
    }


    override fun dispose() {
    }


    private fun createFloorModel(): Model {
        val builder = ModelBuilder()
        builder.begin()
        val texture =
            gamePlayManagers.assetsManager.getTexture("tile_water")
        SharedUtils.createFlatMesh(builder, "floor", 0.5F, texture, 0F)
        return builder.end()
    }

    companion object {
        private const val AMB_PART_CREATION_POSITION_BIAS = 0.05F
        private const val AMB_PART_IMPULSE_COMPONENT = 0.1F
        const val DOORS_DELAY = 2000F
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxVector3 = Vector3()
        private val auxVector4 = Vector3()
        private val auxQuat = Quaternion()
    }
}
