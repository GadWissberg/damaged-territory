package com.gadarts.returnfire.utils

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA
import com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.physics.bullet.Bullet
import com.badlogic.gdx.physics.bullet.collision.CollisionConstants.DISABLE_DEACTIVATION
import com.badlogic.gdx.physics.bullet.collision.CollisionConstants.ISLAND_SLEEPING
import com.badlogic.gdx.physics.bullet.collision.btBoxShape
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject.CollisionFlags
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.badlogic.gdx.physics.bullet.collision.btCompoundShape
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.components.AnimatedTextureComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.amb.AmbComponent
import com.gadarts.returnfire.components.arm.ArmComponent
import com.gadarts.returnfire.components.arm.ArmEffectsData
import com.gadarts.returnfire.components.arm.ArmProperties
import com.gadarts.returnfire.components.arm.ArmRenderData
import com.gadarts.returnfire.components.bullet.BulletBehavior
import com.gadarts.returnfire.components.cd.ChildDecal
import com.gadarts.returnfire.components.character.CharacterColor
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.components.physics.PhysicsComponent
import com.gadarts.returnfire.components.pit.BaseComponent
import com.gadarts.returnfire.components.turret.TurretBaseComponent
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.model.MapGraph
import com.gadarts.returnfire.model.MapGraphCost
import com.gadarts.returnfire.model.MapGraphType
import com.gadarts.returnfire.model.graph.MapGraphNode
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.data.map.InGameTilesLayer
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.SharedUtils.INITIAL_INDEX_OF_TILES_MAPPING
import com.gadarts.shared.assets.definitions.ParticleEffectDefinition
import com.gadarts.shared.assets.definitions.SoundDefinition
import com.gadarts.shared.assets.definitions.external.TextureDefinition
import com.gadarts.shared.assets.definitions.model.AutomaticShapeCreator
import com.gadarts.shared.assets.definitions.model.ModelDefinition
import com.gadarts.shared.assets.map.GameMap
import com.gadarts.shared.assets.map.GameMapTileLayer
import com.gadarts.shared.model.CharacterType
import com.gadarts.shared.model.ElementType
import com.gadarts.shared.model.definitions.*

class MapInflater(
    private val gameSessionData: GameSessionData,
    private val gamePlayManagers: GamePlayManagers,
    private val engine: Engine
) {
    private val ambEntities: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(
            Family.all(AmbComponent::class.java).get()
        )
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
        addFloor(currentMap, exculdedTiles)
        val groundBitMap = gameSessionData.mapData.groundBitMap
        if (GameDebugSettings.PRINT_BIT_MAP) {
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
        gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.MAP_LOADED.ordinal)
    }

    private fun addFloor(
        currentMap: GameMap,
        exculdedTiles: ArrayList<Pair<Int, Int>>
    ) {
        currentMap.layers.forEachIndexed { index, layer ->
            addFloorLayer(exculdedTiles, layer, index)
        }
        gameSessionData.mapData.tilesEntitiesByLayers.forEachIndexed { index, layer ->
            addFloorModelsToCache(layer, index)
        }
    }

    private fun addFloorModelsToCache(layer: InGameTilesLayer, index: Int) {
        val modelCache = layer.modelCache
        modelCache.begin()
        val tilesEntities = layer.tilesEntities
        tilesEntities.forEach { row ->
            row.forEach { tileEntity ->
                if (tileEntity != null) {
                    val tileModelInstance = ComponentsMapper.modelInstance.get(tileEntity)
                    if (tileModelInstance != null) {
                        modelCache.add(tileModelInstance.gameModelInstance.modelInstance)
                        gamePlayManagers.ecs.entityBuilder.addModelCacheComponentToEntity(tileEntity)
                    }
                }
            }
        }
        if (index == 0) {
            addAllExternalSea(tilesEntities[0].size, tilesEntities.size)
        }
        modelCache.end()
    }

    private fun createGraph() {
        val map = gameSessionData.mapData.loadedMap
        val depth = map.depth
        val width = map.width
        val mapGraph = MapGraph(width, depth)
        val occupiedTiles = mutableSetOf<Pair<Int, Int>>()
        engine.getEntitiesFor(
            Family.one(AmbComponent::class.java, TurretBaseComponent::class.java).exclude(BaseComponent::class.java)
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

    private fun addAmbObject(
        position: Vector3,
        def: AmbDefinition,
        exculdedTiles: ArrayList<Pair<Int, Int>>,
        rotation: Float?,
    ) {
        excludeTilesUnderBase(def, position, exculdedTiles)
        if (def.placeInMiddleOfCell) {
            position.add(0.5F, 0F, 0.5F)
        }
        position.sub(def.getModelDefinition().origin)
        val gameModelInstance =
            gamePlayManagers.factories.gameModelInstanceFactory.createGameModelInstance(def.getModelDefinition())
        val entityBuilder = gamePlayManagers.ecs.entityBuilder
            .begin()
            .addModelInstanceComponent(gameModelInstance, position, null, rotation ?: 0F)
            .addDrowningEffectComponent()
            .addAmbComponent(
                if (def.isRandomizeRotation()) MathUtils.random(0F, 360F) else 0F,
                def,
                auxVector1.set(def.getScale(), def.getScale(), def.getScale()),
            )
        applyDecalToModel(def, entityBuilder, gameModelInstance)
        val isGreen = def == AmbDefinition.BASE_GREEN
        val isBrown = def == AmbDefinition.BASE_BROWN
        if (isGreen || isBrown) {
            entityBuilder.addBaseComponent(if (isGreen) CharacterColor.GREEN else CharacterColor.BROWN)
        }
        val entity = entityBuilder.finishAndAddToEngine()
        if (def.collisionFlags >= 0) {
            addPhysicsToObject(
                entity,
                gameModelInstance,
                def.collisionFlags,
                if (def.collisionFlags == CollisionFlags.CF_STATIC_OBJECT) 0F else def.mass,
                4F,
                activationState = if (def.collisionFlags == CollisionFlags.CF_KINEMATIC_OBJECT) DISABLE_DEACTIVATION else ISLAND_SLEEPING
            )
        }
        addAnimationToAmb(def, gameModelInstance, entity)
        if (def == AmbDefinition.PALM_TREE) {
            entityBuilder.addTreeComponentToEntity(entity)
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
        def: AmbDefinition,
        gameModelInstance: GameModelInstance,
        entity: Entity
    ) {
        if (def == AmbDefinition.PALM_TREE) {
            val modelInstance = gameModelInstance.modelInstance
            if (modelInstance.animations.size > 0) {
                gamePlayManagers.ecs.entityBuilder.addAnimationComponentToEntity(entity, modelInstance)
            }
        }
    }

    private fun excludeTilesUnderBase(
        def: AmbDefinition,
        position: Vector3,
        exculdedTiles: ArrayList<Pair<Int, Int>>
    ) {
        if (def == AmbDefinition.BASE_BROWN || def == AmbDefinition.BASE_GREEN) {
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
                GameDebugSettings.HIDE_ENEMIES
            )
            .addCharacterComponent(TurretCharacterDefinition.TURRET_CANNON, CharacterColor.GREEN)
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
        val definition = gameModelInstance.definition
        val modelCollisionShapeInfo = gamePlayManagers.assetsManager.getCachedModelCollisionShapeInfo(definition!!)
        val shape = if (modelCollisionShapeInfo != null) {
            ModelUtils.buildShapeFromModelCollisionShapeInfo(modelCollisionShapeInfo)
        } else {
            createShapeForStaticObject(definition, entity)
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
            .addTurretComponent(baseEntity, false, 0.2F, null)
            .addTurretEnemyAiComponent()
            .addPrimaryArmComponent(
                spark,
                createTurretArmProperties(assetsManager),
                BulletBehavior.REGULAR
            )
            .finishAndAddToEngine()
        ComponentsMapper.turretBase.get(baseEntity).turret = turret
        addPhysicsToTurret(assetsManager, turret, modelInstance.modelInstance)
    }

    private fun addPhysicsToTurret(
        assetsManager: GameAssetManager,
        turret: Entity,
        modelInstance: ModelInstance
    ) {
        val cachedBoundingBox = assetsManager.getCachedBoundingBox(ModelDefinition.TURRET_CANNON)
        val shape = btCompoundShape()
        shape.addChildShape(
            auxMatrix.idt().translate(ModelDefinition.TURRET_CANNON.boundingBoxData.boundingBoxBias), btBoxShape(
                auxVector1.set(
                    cachedBoundingBox.width / 2F,
                    cachedBoundingBox.height / 2F,
                    cachedBoundingBox.depth / 2F
                )
            )
        )
        gamePlayManagers.ecs.entityBuilder.addPhysicsComponentToEntity(
            turret,
            shape,
            10F,
            CollisionFlags.CF_KINEMATIC_OBJECT,
            modelInstance.transform,
        )
    }

    private fun createTurretArmProperties(assetsManager: GameAssetManager): ArmProperties {
        val bulletModelDefinition = ModelDefinition.TANK_CANNON_BULLET
        val particleEffectsPools = gameSessionData.gamePlayData.pools.particleEffectsPools
        return ArmProperties(
            5F,
            assetsManager.getAssetByDefinition(SoundDefinition.CANNON_B),
            5000L,
            10F,
            ArmEffectsData(
                ParticleEffectDefinition.EXPLOSION_MED,
                null,
                particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE_EMIT),
                particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE_UP_LOOP),
            ),
            ArmRenderData(
                bulletModelDefinition,
                assetsManager.getCachedBoundingBox(bulletModelDefinition),
            ),
            true,
            gameSessionData.gamePlayData.pools.rigidBodyPools.obtainRigidBodyPool(bulletModelDefinition),
            -1
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
            ModelDefinition.CANNON_SPARK,
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
                    it.rotation
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

    private fun addFloorLayer(exculdedTiles: ArrayList<Pair<Int, Int>>, layer: GameMapTileLayer, index: Int) {
        val depth = gameSessionData.mapData.loadedMap.depth
        val width = gameSessionData.mapData.loadedMap.width
        val groundBitMap = gameSessionData.mapData.groundBitMap
        val height = calculateLayerHeight(index)

        val entityBuilder = gamePlayManagers.ecs.entityBuilder
        val inGameTilesLayer = gameSessionData.mapData.tilesEntitiesByLayers[index]
        val tilesEntities = inGameTilesLayer.tilesEntities
        for (row in 0 until depth) {
            for (col in 0 until width) {
                if (index == 0 || layer.tiles[row * width + col].code != INITIAL_INDEX_OF_TILES_MAPPING) {
                    addTileEntity(tilesEntities, row, col)
                }
            }
        }

        if (index > 0) {
            for (row in 0 until depth) {
                for (col in 0 until width) {
                    val textureDefinition = MapUtils.determineTextureOfMapPosition(
                        row,
                        col,
                        gamePlayManagers.assetsManager.getTexturesDefinitions(),
                        layer,
                        gameSessionData.mapData.loadedMap
                    )
                    if (groundBitMap[row][col] == 0
                        && (!textureDefinition.fileName.contains("water")
                            || exculdedTiles.contains(Pair(col, row)))
                    ) {
                        groundBitMap[row][col] = 1
                    }
                    if (textureDefinition.fileName.contains("road") && !exculdedTiles.contains(
                            Pair(col, row)
                        )
                    ) {
                        entityBuilder.addRoadComponentToEntity(tilesEntities[row][col]!!, textureDefinition)
                        addTile(Triple(col, height, row), index)
                        exculdedTiles.add(Pair(col, row))
                    }

                }
            }
        }
        addFloorModels(exculdedTiles, height, index)
    }

    private fun addTileEntity(
        tilesEntities: Array<Array<Entity?>>,
        row: Int,
        col: Int
    ) {
        val entityBuilder = gamePlayManagers.ecs.entityBuilder

        val tileEntity = entityBuilder.begin()
            .addGroundComponent()
            .finishAndAddToEngine()
        tilesEntities[row][col] = tileEntity
    }

    private fun calculateLayerHeight(index: Int) = index * 0.0008F

    private fun addTile(
        position: Triple<Int, Float, Int>,
        layerIndex: Int,
        forceFlat: Boolean = false
    ) {
        val x = position.first
        val z = position.third
        val tileEntity = gameSessionData.mapData.tilesEntitiesByLayers[layerIndex].tilesEntities[z][x] ?: return

        val assetsManager = gamePlayManagers.assetsManager
        val layer = gameSessionData.mapData.loadedMap.layers[layerIndex]
        val textureDefinition = MapUtils.determineTextureOfMapPosition(
            z,
            x,
            assetsManager.getTexturesDefinitions(),
            layer,
            gameSessionData.mapData.loadedMap
        )
        var randomDirection = false
        val modelDefinition = if (!forceFlat && textureDefinition.bumpyTile && areNeighboringTilesFlat(x, z, layerIndex)
        ) {
            deleteAllTilesBelow(layerIndex, z, x)
            randomDirection = true
            ModelDefinition.TILE_BUMPY
        } else {
            flatAllTilesBelow(layerIndex, z, x)
            ModelDefinition.TILE_FLAT
        }

        val gameModelInstance =
            gamePlayManagers.factories.gameModelInstanceFactory.createGameModelInstance(modelDefinition)
        gameModelInstance.modelInstance.materials[0].set(BlendingAttribute(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, 1F))
        val entityBuilder = gamePlayManagers.ecs.entityBuilder
        val realPosition = auxVector1.set(x.toFloat() + 0.5F, position.second, z.toFloat() + 0.5F)
        gameModelInstance.modelInstance.transform.setTranslation(realPosition)
        entityBuilder.addModelInstanceComponentToEntity(
            tileEntity,
            gameModelInstance,
            realPosition,
            null,
        )

        if (randomDirection) {
            gameModelInstance.modelInstance.transform.rotate(Vector3.Y, 90F * MathUtils.random(3))
        }

        applyTextureToFloorTile(x, z, tileEntity, gameModelInstance, layer)
    }

    private fun areNeighboringTilesFlat(x: Int, z: Int, layerIndex: Int): Boolean {
        val mapData = gameSessionData.mapData
        val tilesEntities = mapData.tilesEntitiesByLayers[layerIndex].tilesEntities
        if (isTileBumpy(x - 1, z, tilesEntities)) return false
        if (isTileBumpy(x - 1, z + 1, tilesEntities)) return false
        if (isTileBumpy(x, z + 1, tilesEntities)) return false
        if (isTileBumpy(x + 1, z + 1, tilesEntities)) return false
        if (isTileBumpy(x + 1, z, tilesEntities)) return false
        if (isTileBumpy(x + 1, z - 1, tilesEntities)) return false
        if (isTileBumpy(x, z - 1, tilesEntities)) return false
        if (isTileBumpy(x - 1, z - 1, tilesEntities)) return false

        return true
    }

    private fun isTileBumpy(
        x: Int,
        z: Int,
        tilesEntities: Array<Array<Entity?>>,
    ) = (x > 0
        && z > 0
        && x < tilesEntities[0].size
        && z < tilesEntities.size
        && tilesEntities[z][x] != null
        && ComponentsMapper.modelInstance.has(tilesEntities[z][x])
        && ComponentsMapper.modelInstance.get(tilesEntities[z][x]).gameModelInstance.definition == ModelDefinition.TILE_BUMPY)

    private fun flatAllTilesBelow(
        index: Int,
        z: Int,
        x: Int,
    ) {
        deleteAllTilesBelow(index, z, x)
        for (i in 0 until index) {
            val mapData = gameSessionData.mapData
            if (i > 0 && mapData.loadedMap.layers[i].tiles[z * mapData.loadedMap.width + x].code == INITIAL_INDEX_OF_TILES_MAPPING) continue

            addTileEntity(mapData.tilesEntitiesByLayers[i].tilesEntities, z, x)
            addTile(Triple(x, calculateLayerHeight(i), z), i, true)
        }
    }

    private fun deleteAllTilesBelow(layerIndex: Int, z: Int, x: Int) {
        for (i in 0 until layerIndex) {
            deleteTile(i, z, x)
        }
    }

    private fun deleteTile(layerIndex: Int, z: Int, x: Int) {
        val inGameTilesLayer = gameSessionData.mapData.tilesEntitiesByLayers[layerIndex]
        engine.removeEntity(
            inGameTilesLayer.tilesEntities[z][x] ?: return
        )
        inGameTilesLayer.tilesEntities[z][x] = null
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
            activationState = ISLAND_SLEEPING
        )
    }

    private fun applyTextureToFloorTile(
        col: Int,
        row: Int,
        entity: Entity,
        modelInstance: GameModelInstance,
        layer: GameMapTileLayer
    ): TextureDefinition? {
        var textureDefinition: TextureDefinition? = null
        val assetsManager = gamePlayManagers.assetsManager
        if (isPositionInsideBoundaries(row, col)) {
            textureDefinition = MapUtils.determineTextureOfMapPosition(
                row,
                col,
                assetsManager.getTexturesDefinitions(),
                layer,
                gameSessionData.mapData.loadedMap
            )
        }
        if (textureDefinition != null) {
            val texture =
                assetsManager.getTexture(textureDefinition)
            texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            (modelInstance.modelInstance.materials.get(0)
                .get(TextureAttribute.Diffuse) as TextureAttribute)
                .set(TextureRegion(texture))
            if (textureDefinition.animated) {
                applyAnimatedTextureComponentToFloor(textureDefinition, entity)
            }
            if (textureDefinition.fileName.contains("water")) {
                ComponentsMapper.ground.get(entity).isWater = true
            }
        }
        return textureDefinition
    }


    private fun createAndAddGroundTileEntity(modelInstance: GameModelInstance, position: Vector3): Entity {
        return gamePlayManagers.ecs.entityBuilder.begin()
            .addGroundComponent()
            .addModelInstanceComponent(
                modelInstance, position,
                null,
            )
            .finishAndAddToEngine()
    }

    private fun addExtSea(width: Int, depth: Int, x: Float, z: Float) {
        val assetsManager = gamePlayManagers.assetsManager
        val modelInstance = GameModelInstance(
            ModelInstance(ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.TILE_BUMPY))),
            ModelDefinition.TILE_BUMPY
        )
        val entity = createAndAddGroundTileEntity(
            modelInstance,
            auxVector1.set(x, 0F, z)
        )
        gamePlayManagers.ecs.entityBuilder.addModelCacheComponentToEntity(entity)
        modelInstance.modelInstance.transform.scl(width.toFloat(), 1F, depth.toFloat())
        val textureAttribute =
            modelInstance.modelInstance.materials.first()
                .get(TextureAttribute.Diffuse) as TextureAttribute
        initializeExternalSeaTextureAttribute(textureAttribute, width, depth)
        gameSessionData.mapData.tilesEntitiesByLayers[0].modelCache.add(modelInstance.modelInstance)
        val texturesDefinitions = assetsManager.getTexturesDefinitions()
        applyAnimatedTextureComponentToFloor(
            texturesDefinitions.definitions["tile_water"]!!,
            entity
        )
    }

    private fun addAllExternalSea(width: Int, depth: Int) {
        addExtSea(width, EXT_SIZE, width / 2F, -EXT_SIZE / 2F)
        addExtSea(EXT_SIZE, EXT_SIZE, -EXT_SIZE / 2F, -EXT_SIZE / 2F)
        addExtSea(EXT_SIZE, depth, -EXT_SIZE / 2F, depth / 2F)
        addExtSea(EXT_SIZE, EXT_SIZE, -width / 2F, depth + EXT_SIZE / 2F)
        addExtSea(width, EXT_SIZE, width / 2F, depth + EXT_SIZE / 2F)
        addExtSea(EXT_SIZE, EXT_SIZE, width + EXT_SIZE / 2F, depth + EXT_SIZE / 2F)
        addExtSea(EXT_SIZE, depth, width + EXT_SIZE / 2F, depth / 2F)
        addExtSea(EXT_SIZE, EXT_SIZE, width + EXT_SIZE / 2F, -depth / 2F)
    }

    private fun addFloorModels(
        exculdedTiles: ArrayList<Pair<Int, Int>>,
        height: Float,
        index: Int,
    ) {
        val loadedMap = gameSessionData.mapData.loadedMap
        val width = loadedMap.width
        for (row in 0 until loadedMap.depth) {
            for (col in 0 until width) {
                if (!exculdedTiles.contains(Pair(col, row))) {
                    addTile(Triple(col, height, row), index)
                }
            }
        }
    }

    private fun initializeExternalSeaTextureAttribute(
        textureAttribute: TextureAttribute,
        width: Int,
        depth: Int
    ) {
        textureAttribute.textureDescription.uWrap = Texture.TextureWrap.Repeat
        textureAttribute.textureDescription.vWrap = Texture.TextureWrap.Repeat
        textureAttribute.offsetU = 0F
        textureAttribute.offsetV = 0F
        textureAttribute.scaleU = width.toFloat()
        textureAttribute.scaleV = depth.toFloat()
    }

    private fun isPositionInsideBoundaries(row: Int, col: Int) = (row >= 0
        && col >= 0
        && row < gameSessionData.mapData.loadedMap.depth
        && col < gameSessionData.mapData.loadedMap.width)

    private fun applyAnimatedTextureComponentToFloor(
        textureDefinition: TextureDefinition,
        entity: Entity
    ) {
        val frames = com.badlogic.gdx.utils.Array<Texture>()
        for (i in 0 until textureDefinition.frames) {
            frames.add(gamePlayManagers.assetsManager.getTexture(textureDefinition, i))
        }
        val animation = Animation(0.25F, frames)
        animation.playMode = Animation.PlayMode.NORMAL
        entity.add(AnimatedTextureComponent(animation))
    }


    companion object {
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxVector3 = Vector3()
        private val auxMatrix = Matrix4()
        private val auxBoundingBox = BoundingBox()
        private val auxQuat = Quaternion()
        private const val EXT_SIZE = 48
    }
}
