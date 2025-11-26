package com.gadarts.returnfire.utils

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.physics.bullet.Bullet
import com.badlogic.gdx.physics.bullet.collision.Collision
import com.badlogic.gdx.physics.bullet.collision.CollisionConstants.DISABLE_SIMULATION
import com.badlogic.gdx.physics.bullet.collision.CollisionConstants.ISLAND_SLEEPING
import com.badlogic.gdx.physics.bullet.collision.btBoxShape
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject.CollisionFlags
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.badlogic.gdx.physics.bullet.collision.btCompoundShape
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.components.amb.AmbComponent
import com.gadarts.returnfire.ecs.components.arm.ArmComponent
import com.gadarts.returnfire.ecs.components.arm.ArmEffectsData
import com.gadarts.returnfire.ecs.components.arm.ArmProperties
import com.gadarts.returnfire.ecs.components.arm.ArmRenderData
import com.gadarts.returnfire.ecs.components.bullet.BulletBehavior
import com.gadarts.returnfire.ecs.components.cd.ChildDecal
import com.gadarts.returnfire.ecs.components.model.GameModelInstance
import com.gadarts.returnfire.ecs.components.physics.PhysicsComponent
import com.gadarts.returnfire.ecs.components.pit.HangarComponent
import com.gadarts.returnfire.ecs.components.turret.TurretBaseComponent
import com.gadarts.returnfire.ecs.systems.EntityBuilder
import com.gadarts.returnfire.ecs.systems.data.session.GameSessionData
import com.gadarts.returnfire.graph.MapGraph
import com.gadarts.returnfire.graph.MapGraphCost
import com.gadarts.returnfire.graph.MapGraphNode
import com.gadarts.returnfire.graph.MapGraphType
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.SharedUtils
import com.gadarts.shared.assets.definitions.ParticleEffectDefinition
import com.gadarts.shared.assets.definitions.SoundDefinition
import com.gadarts.shared.assets.definitions.model.AutomaticShapeCreator
import com.gadarts.shared.assets.definitions.model.ModelDefinition
import com.gadarts.shared.data.CharacterColor
import com.gadarts.shared.data.ImmutableGameModelInstanceInfo
import com.gadarts.shared.data.creation.MapInflater
import com.gadarts.shared.data.definitions.AmbDefinition
import com.gadarts.shared.data.definitions.ElementDefinition
import com.gadarts.shared.data.definitions.characters.CharacterDefinition
import com.gadarts.shared.data.definitions.characters.SimpleCharacterDefinition
import com.gadarts.shared.data.definitions.characters.TurretCharacterDefinition
import com.gadarts.shared.data.type.CharacterType
import com.gadarts.shared.data.type.ElementType

class MapInflaterImpl(
    private val gameSessionData: GameSessionData,
    private val gamePlayManagers: GamePlayManagers,
    private val engine: Engine
) : MapInflater {
    private val groundInflater = GroundInflater(gameSessionData, gamePlayManagers, engine)
    private val ambEntities: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(
            Family.all(AmbComponent::class.java).get()
        )
    }

    override fun addAmbObject(
        position: Vector3,
        ambDefinition: AmbDefinition,
        exculdedTiles: ArrayList<Pair<Int, Int>>,
        rotation: Float?,
        color: CharacterColor?,
    ) {
        ambDefinition.onCreation?.invoke(this, auxVector3.set(position), ambDefinition, exculdedTiles)
        excludeTilesUnderBase(ambDefinition, position, exculdedTiles)
        if (ambDefinition.placeInMiddleOfCell) {
            position.add(0.5F, 0F, 0.5F)
        }
        position.sub(ambDefinition.getModelDefinition().origin)
        val gameModelInstance =
            gamePlayManagers.factories.gameModelInstanceFactory.createGameModelInstance(
                ambDefinition.getModelDefinition(),
                ambDefinition.customTexturePerColor,
                color
            )
        val entityBuilder = gamePlayManagers.ecs.entityBuilder
            .begin()
            .addModelInstanceComponent(
                model = gameModelInstance,
                position = position,
                boundingBox = null,
                direction = rotation ?: 0F,
                haloEffect = ambDefinition.outlineEffect
            )
            .addDrowningEffectComponent()
            .addAmbComponent(
                if (ambDefinition.isRandomizeRotation()) MathUtils.random(0F, 360F) else 0F,
                ambDefinition,
                auxVector1.set(ambDefinition.getScale(), ambDefinition.getScale(), ambDefinition.getScale()),
            )
        applyDecalToModel(ambDefinition, entityBuilder, gameModelInstance)
        if (ambDefinition == AmbDefinition.OPPONENT_BASE && color != null) {
            entityBuilder.addHangarComponent(color)
        }
        val entity = entityBuilder.finishAndAddToEngine()
        if (ambDefinition.collisionFlags >= 0) {
            addPhysicsToObject(
                entity,
                gameModelInstance,
                ambDefinition.collisionFlags,
                if (ambDefinition.collisionFlags == CollisionFlags.CF_STATIC_OBJECT) 0F else ambDefinition.mass,
                4F,
                activationState = Collision.DISABLE_SIMULATION
            )
        }
        addAnimationToAmb(gameModelInstance, entity)
        if (ambDefinition == AmbDefinition.PALM_TREE) {
            entityBuilder.addTreeComponentToEntity(entity)
        } else if (ambDefinition.getModelDefinition() == ModelDefinition.FLAG) {
            entityBuilder.addFlagComponentToEntity(entity, color!!)
        } else if (ambDefinition.getModelDefinition() == ModelDefinition.FLAG_FLOOR) {
            entityBuilder.addFlagFloorComponentToEntity(entity, color!!)
        }
    }

    fun inflate() {
        val exculdedTiles = ArrayList<Pair<Int, Int>>()
        addAmbEntities(exculdedTiles)
        val currentMap = gameSessionData.mapData.loadedMap
        val depth = currentMap.depth
        val width = currentMap.width
        gameSessionData.mapData.groundBitMap = Array(
            depth
        ) { Array(width) { 0 } }
        groundInflater.addGround(currentMap, exculdedTiles)
        val groundBitMap = gameSessionData.mapData.groundBitMap
        if (gamePlayManagers.assetsManager.gameSettings.printBitMap) {
            for (y in 0 until depth) {
                for (x in 0 until width) {
                    print("${groundBitMap[y][x]} ")
                }
                println()
            }
        }
        groundBitMap.forEachIndexed { row, cols ->
            cols.forEachIndexed { col, tileBit ->
                if (tileBit == 1) {
                    val tilesEntitiesByLayers = gameSessionData.mapData.tilesEntitiesByLayers
                    for (i in tilesEntitiesByLayers.size - 1 downTo 0) {
                        val entity = tilesEntitiesByLayers[i].tilesEntities[row][col]
                        if (entity != null) {
                            addPhysicsToTile(
                                entity,
                                auxVector1.set(
                                    col.toFloat() + 0.5F,
                                    0F,
                                    row.toFloat() + 0.5F
                                )
                            )
                            break
                        }
                    }
                }
            }
        }
        addCharacters()
        createGraph()
    }


    private fun createGraph() {
        val map = gameSessionData.mapData.loadedMap
        val depth = map.depth
        val width = map.width
        val mapGraph = MapGraph(width, depth)
        val occupiedTiles = mutableSetOf<Pair<Int, Int>>()
        engine.getEntitiesFor(
            Family.one(AmbComponent::class.java, TurretBaseComponent::class.java).exclude(HangarComponent::class.java)
                .get()
        ).forEach {
            if (MapUtils.isEntityMarksNodeAsBlocked(it)) {
                val ambComponent = ComponentsMapper.amb.get(it)
                val output = mutableListOf<Pair<Int, Int>>()
                if (ambComponent == null || !ambComponent.def.forceSingleNodeForMarksNodeAsBlocked) {
                    MapUtils.getTilesCoveredByBoundingBox(it, mapGraph) { x, z, _ ->
                        output.add(Pair(x, z))
                    }
                } else {
                    val position =
                        ComponentsMapper.modelInstance.get(it).gameModelInstance.modelInstance.transform.getTranslation(
                            auxVector1
                        )
                    output.add(
                        Pair(
                            position.x.toInt(), position.z.toInt()
                        )
                    )
                }
                occupiedTiles.addAll(output)
            }
        }
        createGraphNodes(width, depth, mapGraph, occupiedTiles)
        connectGraphNodes(width, depth, mapGraph)
        gameSessionData.mapData.mapGraph = mapGraph
    }

    private fun connectGraphNodes(
        width: Int,
        depth: Int,
        mapGraph: MapGraph,
    ) {
        val tilesMapping = gameSessionData.mapData.loadedMap
        for (i in 0 until width * depth) {
            val x = i % width
            val y = i / width
            val from = mapGraph.getNode(x, y)

            if (x > 0) {
                connect(mapGraph, from, x - 1, y, MapGraphCost.FREE_WAY)
            }
            if (x > 0 && y < tilesMapping.depth - 1) {
                connect(mapGraph, from, x - 1, y + 1, calculateDiagonalCost(mapGraph, x - 1, y, x, y + 1))
            }
            if (y < tilesMapping.depth - 1) {
                connect(mapGraph, from, x, y + 1, MapGraphCost.FREE_WAY)
            }
            if (x < tilesMapping.width - 1 && y < tilesMapping.depth - 1) {
                connect(mapGraph, from, x + 1, y + 1, calculateDiagonalCost(mapGraph, x + 1, y, x, y + 1))
            }
            if (x < tilesMapping.width - 1) {
                connect(mapGraph, from, x + 1, y, MapGraphCost.FREE_WAY)
            }
            if (x < tilesMapping.width - 1 && y > 0) {
                connect(mapGraph, from, x + 1, y - 1, calculateDiagonalCost(mapGraph, x + 1, y, x, y - 1))
            }
            if (y > 0) {
                connect(mapGraph, from, x, y - 1, MapGraphCost.FREE_WAY)
            }
            if (x > 0 && y > 0) {
                connect(
                    mapGraph, from, x - 1, y - 1, calculateDiagonalCost(mapGraph, x - 1, y, x, y - 1)
                )
            }
        }
    }

    private fun calculateDiagonalCost(
        mapGraph: MapGraph,
        x1: Int,
        y1: Int,
        x2: Int,
        y2: Int
    ): MapGraphCost {
        return if (mapGraph.getNode(x1, y1).type == MapGraphType.AVAILABLE
            && mapGraph.getNode(x2, y2).type == MapGraphType.AVAILABLE
        ) MapGraphCost.FREE_WAY else MapGraphCost.BLOCKED_DIAGONAL
    }

    private fun connect(
        mapGraph: MapGraph,
        from: MapGraphNode,
        toX: Int,
        toY: Int,
        cost: MapGraphCost,
    ) {
        val to = mapGraph.getNode(toX, toY)
        mapGraph.connect(
            from,
            to,
            if (from.type == MapGraphType.WATER || to.type == MapGraphType.WATER) MapGraphCost.BLOCKED_WAY else cost
        )
    }

    private fun createGraphNodes(
        width: Int,
        depth: Int,
        mapGraph: MapGraph,
        occupiedTiles: MutableSet<Pair<Int, Int>>
    ) {
        for (y in 0 until depth) {
            for (x in 0 until width) {
                val type = if (gameSessionData.mapData.groundBitMap[y][x] == 0) {
                    MapGraphType.WATER
                } else if (occupiedTiles.contains(x to y)) {
                    MapGraphType.BLOCKED
                } else {
                    MapGraphType.AVAILABLE
                }
                mapGraph.addNode(
                    x, y, type
                )
            }
        }
    }

    private fun applyTransformOnAmbEntities() {
        ambEntities.forEach {
            if (!ComponentsMapper.modelInstance.has(it)) return
            val scale = ComponentsMapper.amb.get(it).getScale(auxVector1)
            val transform =
                ComponentsMapper.modelInstance.get(it).gameModelInstance.modelInstance.transform
            transform.scl(scale).rotate(Vector3.Y, ComponentsMapper.amb.get(it).rotation)
        }
    }

    private fun applyDecalToModel(
        def: AmbDefinition,
        entityBuilder: EntityBuilder,
        gameModelInstance: GameModelInstance
    ) {
        val decal = def.getModelDefinition().decal
        if (decal != null) {
            val localRotation = gameModelInstance.modelInstance.transform.getRotation(Quaternion())
            entityBuilder.addChildDecalComponent(
                listOf(
                    ChildDecal(
                        Decal.newDecal(
                            1F, 1F,
                            TextureRegion(
                                gamePlayManagers.assetsManager.getTexture(
                                    decal
                                ),
                            ),
                            true
                        ),
                        Vector3.Zero,
                        localRotation.setEulerAngles(localRotation.yaw + 90F, localRotation.pitch, localRotation.roll)
                    )
                )
            )
        }
    }

    private fun addAnimationToAmb(
        gameModelInstance: GameModelInstance,
        entity: Entity,
    ) {
        val modelInstance = gameModelInstance.modelInstance
        if (modelInstance.animations.size > 0) {
            gamePlayManagers.ecs.entityBuilder.addAnimationComponentToEntity(
                entity,
                gameModelInstance.gameModelInstanceInfo?.getDefinition()?.loopAnimation ?: false,
                modelInstance
            )
        }
    }

    private fun excludeTilesUnderBase(
        def: AmbDefinition,
        position: Vector3,
        exculdedTiles: ArrayList<Pair<Int, Int>>,
    ) {
        if (def == AmbDefinition.OPPONENT_BASE) {
            val x = position.x.toInt()
            val z = position.z.toInt()
            exculdedTiles.add(Pair(x, z))
            exculdedTiles.add(Pair(x + 1, z))
            exculdedTiles.add(Pair(x, z + 1))
            exculdedTiles.add(Pair(x + 1, z + 1))
        }
    }

    private fun addCharacter(
        position: Vector3,
        characterDefinition: CharacterDefinition,
    ) {
        val gameModelInstance =
            gamePlayManagers.factories.gameModelInstanceFactory.createGameModelInstance(characterDefinition.getModelDefinition())
        val entityBuilder = gamePlayManagers.ecs.entityBuilder.begin()
            .addModelInstanceComponent(
                gameModelInstance,
                position,
                null,
                0F,
                gamePlayManagers.assetsManager.gameSettings.hideEnemies
            )
            .addCharacterComponent(TurretCharacterDefinition.GUARD_TURRET_CANNON, CharacterColor.GREEN)
        GeneralUtils.addColorComponent(
            entityBuilder,
            CharacterColor.GREEN
        )
        val baseEntity = entityBuilder
            .addBaseAiComponent(characterDefinition.getHP())
            .addTurretBaseComponent()
            .finishAndAddToEngine()
        addPhysicsToObject(
            baseEntity,
            gameModelInstance,
            CollisionFlags.CF_STATIC_OBJECT,
            0F,
        )
        if (characterDefinition.getCharacterType() == CharacterType.TURRET) {
            addTurret(baseEntity)
        }
    }


    private fun addPhysicsToObject(
        entity: Entity,
        gameModelInstance: GameModelInstance,
        collisionFlags: Int,
        mass: Float,
        friction: Float = 1.5F,
        activationState: Int = ISLAND_SLEEPING
    ): PhysicsComponent {
        val definition = gameModelInstance.gameModelInstanceInfo?.modelDefinition
        val modelCollisionShapeInfo =
            gamePlayManagers.assetsManager.getCachedModelCollisionShapeInfo(
                gameModelInstance.gameModelInstanceInfo!!
            )
        val shape = if (modelCollisionShapeInfo != null) {
            SharedUtils.buildShapeFromModelCollisionShapeInfo(modelCollisionShapeInfo)
        } else {
            createShapeForStaticObject(definition!!, entity)
        }
        return gamePlayManagers.ecs.entityBuilder.addPhysicsComponentToEntity(
            entity = entity,
            shape = shape,
            mass = mass,
            collisionFlag = collisionFlags,
            transform = gameModelInstance.modelInstance.transform,
            gravityScalar = 1F,
            friction = friction,
            activationState = activationState
        )
    }


    private fun createShapeForStaticObject(modelDefinition: ModelDefinition, entity: Entity): btCollisionShape {
        val shape: btCollisionShape
        when (val physicalShapeCreator = modelDefinition.physicsData.physicalShapeCreator) {
            null -> {
                shape = btCompoundShape()
                val dimensions =
                    auxBoundingBox.set(gamePlayManagers.assetsManager.getCachedBoundingBox(modelDefinition))
                        .getDimensions(
                            auxVector3
                        )
                val btBoxShape = btBoxShape(
                    dimensions.scl(0.5F)
                )
                shape.addChildShape(
                    auxMatrix.idt().translate(0F, dimensions.y / 2F, 0F), btBoxShape
                )
            }

            is AutomaticShapeCreator -> {
                shape = Bullet.obtainStaticNodeShape(
                    ComponentsMapper.modelInstance.get(entity).gameModelInstance.modelInstance.nodes[0],
                    true
                )
            }

            else -> {
                shape = physicalShapeCreator.create()
            }
        }
        return shape
    }

    private fun addTurret(
        baseEntity: Entity
    ) {
        val assetsManager = gamePlayManagers.assetsManager
        val modelInstance =
            gamePlayManagers.factories.gameModelInstanceFactory.createGameModelInstance(ModelDefinition.TURRET_CANNON)
        val spark = addTurretSpark(assetsManager, modelInstance.modelInstance)
        val turret = gamePlayManagers.ecs.entityBuilder.begin()
            .addBaseAiComponent(0F)
            .addModelInstanceComponent(
                modelInstance,
                calculateTurretPosition(baseEntity, assetsManager),
                null,
            )
            .addTurretComponent(
                base = baseEntity,
                followBasePosition = false,
                followBaseRotation = false,
                relativeY = 0.2F,
                cannon = null
            )
            .addTurretEnemyAiComponent()
            .addPrimaryArmComponent(
                spark,
                createTurretArmProperties(assetsManager),
                BulletBehavior.REGULAR
            )
            .finishAndAddToEngine()
        ComponentsMapper.turretBase.get(baseEntity).turret = turret
        addPhysicsToTurret(turret, modelInstance)
    }

    private fun addPhysicsToTurret(
        turret: Entity,
        modelInstance: GameModelInstance
    ) {
        val modelCollisionShapeInfo =
            gamePlayManagers.assetsManager.getCachedModelCollisionShapeInfo(
                modelInstance.gameModelInstanceInfo!!
            )
        val shape = SharedUtils.buildShapeFromModelCollisionShapeInfo(modelCollisionShapeInfo!!)
        gamePlayManagers.ecs.entityBuilder.addPhysicsComponentToEntity(
            turret,
            shape,
            10F,
            CollisionFlags.CF_KINEMATIC_OBJECT,
            modelInstance.modelInstance.transform,
        )
    }

    private fun createTurretArmProperties(assetsManager: GameAssetManager): ArmProperties {
        val bulletModelDefinition = ModelDefinition.TANK_CANNON_BULLET
        val particleEffectsPools = gameSessionData.gamePlayData.pools.particleEffectsPools
        return ArmProperties(
            damage = 5F,
            shootingSound = assetsManager.getAssetByDefinition(SoundDefinition.CANNON_B),
            reloadDuration = 5000L,
            speed = 10F,
            effectsData = ArmEffectsData(
                ParticleEffectDefinition.EXPLOSION_MED,
                null,
                particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE_EMIT),
                particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE_UP_LOOP),
            ),
            renderData = ArmRenderData(
                bulletModelDefinition,
                assetsManager.getCachedBoundingBox(bulletModelDefinition),
            ),
            explosive = true,
            rigidBodyPool = gameSessionData.gamePlayData.pools.rigidBodyPools.obtainRigidBodyPool(bulletModelDefinition),
            ammo = -1,
            destroyOnSky = false
        )
    }

    private fun calculateTurretPosition(
        baseEntity: Entity,
        assetsManager: GameAssetManager
    ): Vector3 {
        return ComponentsMapper.physics.get(baseEntity).rigidBody.worldTransform.getTranslation(
            auxVector1
        ).add(0F, assetsManager.getCachedBoundingBox(ModelDefinition.TURRET_BASE).height, 0F)
    }

    private fun addTurretSpark(
        assetsManager: GameAssetManager,
        modelInstance: ModelInstance
    ): Entity {
        val model = GameModelInstance(
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.CANNON_SPARK)),
            ImmutableGameModelInstanceInfo(ModelDefinition.CANNON_SPARK),
        )
        val spark = gamePlayManagers.ecs.entityBuilder.begin()
            .addModelInstanceComponent(
                model,
                Vector3(),
                assetsManager.getCachedBoundingBox(ModelDefinition.TURRET_CANNON),
                hidden = true
            )
            .addSparkComponent(createRelativePositionCalculatorForTurret(modelInstance))
            .finishAndAddToEngine()
        return spark
    }

    private fun createRelativePositionCalculatorForTurret(modelInstance: ModelInstance) =
        object : ArmComponent.RelativePositionCalculator {
            override fun calculate(parent: Entity, output: Vector3): Vector3 {
                return output.setZero()
                    .add(
                        0.7F,
                        0F,
                        ComponentsMapper.turret.get(parent).updateCurrentShootingArm() * 0.3F
                    )
                    .rot(modelInstance.transform)
            }
        }


    private fun addCharacters() {
        gameSessionData.mapData.loadedMap.objects.filter {
            val definition = it.definition
            val elementDefinition = ElementType.CHARACTER.definitions.find { def ->
                def.getName().lowercase() == definition
            }
            it.type == ElementType.CHARACTER
                    && elementDefinition != SimpleCharacterDefinition.APACHE
                    && elementDefinition != TurretCharacterDefinition.TANK
        }
            .forEach {
                val elementDefinition = stringToDefinition(it.definition, ElementType.CHARACTER)
                addCharacter(
                    auxVector2.set(it.column.toFloat() + 0.5F, 0.01F, it.row.toFloat() + 0.5F),
                    elementDefinition as CharacterDefinition,
                )
            }
    }

    private fun stringToDefinition(definition: String, type: ElementType): ElementDefinition? {
        val elementDefinition = type.definitions.find { def ->
            def.getName().lowercase() == definition.lowercase()
        }
        return elementDefinition
    }

    private fun addAmbEntities(exculdedTiles: ArrayList<Pair<Int, Int>>) {
        gameSessionData.mapData.loadedMap.objects.filter { it.type == ElementType.AMB }
            .forEach {
                val stringToDefinition = stringToDefinition(it.definition, ElementType.AMB)
                if (stringToDefinition == null) {
                    Gdx.app.log(
                        "MapInflater",
                        "Could not find definition for amb object: ${it.definition} at (${it.column}, ${it.row})"
                    )
                }
                addAmbObject(
                    auxVector2.set(it.column.toFloat(), 0.02F, it.row.toFloat()),
                    stringToDefinition as AmbDefinition,
                    exculdedTiles,
                    it.rotation,
                    it.color
                )
            }
        applyTransformOnAmbEntities()
        initializeFenceNeighbours()
    }

    private fun initializeFenceNeighbours() {
        val fences = ambEntities.filter { ComponentsMapper.amb.get(it).def == AmbDefinition.FENCE }
        val fenceGrid = createFenceGrid(fences)
        fences.forEach {
            val transform = ComponentsMapper.modelInstance.get(it).gameModelInstance.modelInstance.transform
            val position = transform.getTranslation(auxVector1)
            val yaw = transform.getRotation(auxQuat).yaw
            val row = position.z.toInt()
            val col = position.x.toInt()
            val depth = gameSessionData.mapData.loadedMap.depth
            val width = gameSessionData.mapData.loadedMap.width
            if (MathUtils.isEqual(yaw, 0F, 1F)) {
                ComponentsMapper.fence.get(it).setNeighbors(
                    if (row > 0) fenceGrid[row - 1][col] else null,
                    if (row < depth - 1) fenceGrid[row + 1][col] else null
                )
            } else if (MathUtils.isEqual(yaw, 180F, 1F)) {
                ComponentsMapper.fence.get(it).setNeighbors(
                    if (row < depth - 1) fenceGrid[row + 1][col] else null,
                    if (row > 0) fenceGrid[row - 1][col] else null,
                )
            } else if (MathUtils.isEqual(yaw, 90F, 1F)) {
                ComponentsMapper.fence.get(it).setNeighbors(
                    if (col > 0) fenceGrid[row][col - 1] else null,
                    if (col < width - 1) fenceGrid[row][col + 1] else null
                )
            } else if (MathUtils.isEqual(yaw, 270F, 1F)) {
                ComponentsMapper.fence.get(it).setNeighbors(
                    if (col < width - 1) fenceGrid[row][col + 1] else null,
                    if (col > 0) fenceGrid[row][col - 1] else null,
                )

            }
        }
    }

    private fun createFenceGrid(
        fences: List<Entity>
    ): Array<Array<Entity?>> {
        val loadedMap = gameSessionData.mapData.loadedMap
        val fenceGrid = Array<Array<Entity?>>(loadedMap.depth) { arrayOfNulls(loadedMap.width) }
        fences.forEach {
            val position =
                ComponentsMapper.modelInstance.get(it).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector1
                )
            val x = position.x.toInt()
            val z = position.z.toInt()
            gamePlayManagers.ecs.entityBuilder.addFenceComponentToEntity(it)
            fenceGrid[z][x] = it
        }
        return fenceGrid
    }


    private fun addPhysicsToTile(
        entity: Entity,
        position: Vector3
    ) {
        val entityBuilder = gamePlayManagers.ecs.entityBuilder

        entityBuilder.addPhysicsComponentToEntity(
            entity,
            btBoxShape(Vector3(0.5F, 0.1F, 0.5F)),
            0F,
            CollisionFlags.CF_STATIC_OBJECT,
            auxMatrix.idt().translate(position),
            activationState = DISABLE_SIMULATION
        )
    }


    companion object {
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxVector3 = Vector3()
        private val auxMatrix = Matrix4()
        private val auxBoundingBox = BoundingBox()
        private val auxQuat = Quaternion()
    }
}
