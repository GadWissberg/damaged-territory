package com.gadarts.returnfire.systems.map

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.MathUtils.random
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.physics.bullet.collision.btBoxShape
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject
import com.badlogic.gdx.physics.bullet.collision.btCompoundShape
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.GeneralUtils
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.assets.definitions.external.TextureDefinition
import com.gadarts.returnfire.components.*
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.components.physics.PhysicsComponent
import com.gadarts.returnfire.model.AmbDefinition
import com.gadarts.returnfire.model.CharactersDefinitions
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.events.data.PhysicsCollisionEventData

class MapSystem : GameEntitySystem() {

    private val waterSplashSounds by lazy { managers.assetsManager.getAllAssetsByDefinition(SoundDefinition.WATER_SPLASH) }
    private val waterSplashFloorTexture: Texture by lazy { managers.assetsManager.getTexture("water_splash_floor") }
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
                WaterWaveComponent::class.java,
            ).get()
        )
    }
    private val floors: Array<Array<Entity?>> by lazy {
        val tilesMapping = gameSessionData.currentMap.tilesMapping
        Array(tilesMapping.size) { arrayOfNulls(tilesMapping[0].size) }
    }
    private val ambEntities: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(
            Family.all(AmbComponent::class.java).get()
        )
    }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> =
        mapOf(SystemEvents.PHYSICS_COLLISION to object : HandlerOnEvent {
            override fun react(msg: Telegram, gameSessionData: GameSessionData, managers: Managers) {
                handleCollisionBulletAndGround(PhysicsCollisionEventData.colObj0, PhysicsCollisionEventData.colObj1)
            }
        }

        )

    private fun handleCollisionBulletAndGround(
        collisionObject0: btCollisionObject, collisionObject1: btCollisionObject
    ) {
        val bullet = collisionObject0.userData as Entity
        if (ComponentsMapper.bullet.has(bullet) && ComponentsMapper.ground.has(collisionObject1.userData as Entity)) {
            val position =
                ComponentsMapper.modelInstance.get(bullet).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector1
                )
            if (ComponentsMapper.ground.get(floors[position.z.toInt()][position.x.toInt()]).water) {
                addWaterSplash(position)
            } else {
                val explosion = ComponentsMapper.bullet.get(bullet).explosion
                EntityBuilder.begin()
                    .addParticleEffectComponent(
                        position,
                        gameSessionData.pools.particleEffectsPools.obtain(
                            explosion,
                        )
                    ).finishAndAddToEngine()
            }
        }
    }

    private fun addWaterSplash(position: Vector3) {
        EntityBuilder.begin()
            .addParticleEffectComponent(
                position,
                gameSessionData.pools.particleEffectsPools.obtain(
                    ParticleEffectDefinition.WATER_SPLASH
                )
            ).finishAndAddToEngine()
        val gameModelInstance = gameSessionData.waterWavePool.obtain()
        managers.soundPlayer.play(
            waterSplashSounds.random(),
        )
        val material = gameModelInstance.modelInstance.materials.get(0)
        val blendingAttribute = material.get(BlendingAttribute.Type) as BlendingAttribute
        blendingAttribute.opacity = 1F
        val textureAttribute =
            material.get(TextureAttribute.Diffuse) as TextureAttribute
        textureAttribute.textureDescription.texture = waterSplashFloorTexture
        EntityBuilder.begin()
            .addModelInstanceComponent(gameModelInstance, position, false)
            .addWaterWaveComponent()
            .finishAndAddToEngine()
        gameModelInstance.modelInstance.transform.scl(0.5F)
    }

    override fun initialize(gameSessionData: GameSessionData, managers: Managers) {
        super.initialize(gameSessionData, managers)
        gameSessionData.floorModel = createFloorModel()
        gameSessionData.renderData.modelCache = ModelCache()
        addFloor()
    }


    override fun onSystemReady() {
        super.onSystemReady()
        addAmbEntities(gameSessionData)
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
            (ComponentsMapper.modelInstance.get(entity).gameModelInstance.modelInstance.materials.get(0)
                .get(TextureAttribute.Diffuse) as TextureAttribute).textureDescription.texture =
                keyFrame
        }
        waterSplashEntitiesToRemove.clear()
        for (entity in waterSplashEntities) {
            if (TimeUtils.timeSinceMillis(ComponentsMapper.waterWave.get(entity).creationTime) > 1000L) {
                waterSplashEntitiesToRemove.add(entity)
            } else {
                val modelInstance = ComponentsMapper.modelInstance.get(entity).gameModelInstance.modelInstance
                modelInstance.transform.scl(1.01F, 1.01F, 1.01F)
                val blendAttribute = modelInstance.materials.get(0).get(BlendingAttribute.Type) as BlendingAttribute
                blendAttribute.opacity -= 0.01F
            }
        }
        while (!waterSplashEntitiesToRemove.isEmpty) {
            val entity = waterSplashEntitiesToRemove.removeIndex(0)
            gameSessionData.waterWavePool.free(ComponentsMapper.modelInstance.get(entity).gameModelInstance)
            engine.removeEntity(entity)
        }
    }

    override fun dispose() {
        gameSessionData.renderData.modelCache.dispose()
    }

    private fun addAmbEntities(gameSessionData: GameSessionData) {
        gameSessionData.currentMap.placedElements.forEach {
            if (it.definition != CharactersDefinitions.PLAYER) {
                addAmbObject(
                    auxVector2.set(it.col.toFloat() + 0.5F, 0.01F, it.row.toFloat() + 0.5F),
                    it.definition as AmbDefinition,
                    it.direction
                )
            }
        }
        applyTransformOnAmbEntities()
    }

    private fun createFloorModel(): Model {
        val builder = ModelBuilder()
        builder.begin()
        val texture =
            managers.assetsManager.getTexture("tile_water")
        GeneralUtils.createFlatMesh(builder, "floor", 0.5F, texture, 0F)
        return builder.end()
    }

    private fun addFloor() {
        gameSessionData.renderData.modelCache.begin()
        val tilesMapping = gameSessionData.currentMap.tilesMapping
        val depth = tilesMapping.size
        val width = tilesMapping[0].size
        addFloorRegion(depth, width)
        addAllExternalSea(width, depth)
        gameSessionData.renderData.modelCache.end()
    }

    private fun addAllExternalSea(width: Int, depth: Int) {
        addExtSea(width, EXT_SIZE, width / 2F, -EXT_SIZE / 2F)
        addExtSea(EXT_SIZE, EXT_SIZE, -EXT_SIZE / 2F, -EXT_SIZE / 2F)
        addExtSea(EXT_SIZE, depth, -width / 2F, depth / 2F)
        addExtSea(EXT_SIZE, EXT_SIZE, -width / 2F, depth + EXT_SIZE / 2F)
        addExtSea(width, EXT_SIZE, width / 2F, depth + EXT_SIZE / 2F)
        addExtSea(EXT_SIZE, EXT_SIZE, width + EXT_SIZE / 2F, depth + EXT_SIZE / 2F)
        addExtSea(EXT_SIZE, depth, width + EXT_SIZE / 2F, depth / 2F)
        addExtSea(EXT_SIZE, EXT_SIZE, width + EXT_SIZE / 2F, -depth / 2F)
    }

    private fun addExtSea(width: Int, depth: Int, x: Float, z: Float) {
        val modelInstance = GameModelInstance(ModelInstance(gameSessionData.floorModel), definition = null)
        val entity = createAndAddGroundTileEntity(
            modelInstance,
            auxVector1.set(x, 0F, z)
        )
        modelInstance.modelInstance.transform.scl(width.toFloat(), 1F, depth.toFloat())
        val textureAttribute =
            modelInstance.modelInstance.materials.first()
                .get(TextureAttribute.Diffuse) as TextureAttribute
        initializeExternalSeaTextureAttribute(textureAttribute, width, depth)
        gameSessionData.renderData.modelCache.add(modelInstance.modelInstance)
        val texturesDefinitions = managers.assetsManager.getTexturesDefinitions()
        applyAnimatedTextureComponentToFloor(texturesDefinitions.definitions["tile_water"]!!, entity)
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

    private fun addFloorRegion(
        rows: Int,
        cols: Int,
    ) {
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                addFloorTile(
                    row,
                    col,
                    GameModelInstance(
                        ModelInstance(gameSessionData.floorModel),
                        definition = null
                    )
                )
            }
        }
    }

    private fun addFloorTile(
        row: Int,
        col: Int,
        modelInstance: GameModelInstance
    ) {
        val entity = createAndAddGroundTileEntity(
            modelInstance,
            auxVector1.set(col.toFloat() + 0.5F, 0F, row.toFloat() + 0.5F)
        )
        gameSessionData.renderData.modelCache.add(modelInstance.modelInstance)
        applyTextureToFloorTile(col, row, entity, modelInstance)
    }

    private fun applyTextureToFloorTile(
        col: Int,
        row: Int,
        entity: Entity,
        modelInstance: GameModelInstance
    ) {
        var textureDefinition: TextureDefinition? = null
        val playerPosition =
            ComponentsMapper.modelInstance.get(gameSessionData.player).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector1
            )
        val texturesDefinitions =
            managers.assetsManager.getTexturesDefinitions()
        if (playerPosition.x.toInt() == col && playerPosition.z.toInt() == row) {
            textureDefinition = texturesDefinitions.definitions["base_door"]
        } else if (isPositionInsideBoundaries(row, col)) {
            floors[row][col] = entity
            val definitions = managers.assetsManager.getTexturesDefinitions()
            val indexOfFirst =
                TILES_CHARS.indexOfFirst { c: Char -> gameSessionData.currentMap.tilesMapping[row][col] == c }
            textureDefinition =
                definitions.definitions[TilesMapping.tiles[indexOfFirst]]
        }
        if (textureDefinition != null) {
            val texture =
                managers.assetsManager.getTexture(textureDefinition)
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            (modelInstance.modelInstance.materials.get(0).get(TextureAttribute.Diffuse) as TextureAttribute)
                .set(TextureRegion(texture))
            if (textureDefinition.animated) {
                applyAnimatedTextureComponentToFloor(textureDefinition, entity)
            }
            if (textureDefinition.fileName.contains("water")) {
                ComponentsMapper.ground.get(entity).water = true
            }
        }
    }

    private fun isPositionInsideBoundaries(row: Int, col: Int) = (row >= 0
        && col >= 0
        && row < gameSessionData.currentMap.tilesMapping.size
        && col < gameSessionData.currentMap.tilesMapping[0].size)

    private fun applyAnimatedTextureComponentToFloor(
        textureDefinition: TextureDefinition,
        entity: Entity
    ) {
        val frames = com.badlogic.gdx.utils.Array<Texture>()
        for (i in 0 until textureDefinition.frames) {
            frames.add(managers.assetsManager.getTexture(textureDefinition, i))
        }
        val animation = Animation(0.25F, frames)
        animation.playMode = Animation.PlayMode.NORMAL
        entity.add(AnimatedTextureComponent(animation))
    }

    private fun createAndAddGroundTileEntity(
        modelInstance: GameModelInstance,
        position: Vector3
    ): Entity {
        return EntityBuilder.begin()
            .addModelInstanceComponent(modelInstance, position, true)
            .addGroundComponent()
            .finishAndAddToEngine()
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
        direction: Int,
    ) {
        val randomScale = if (def.isRandomizeScale()) random(MIN_SCALE, MAX_SCALE) else 1F
        val modelDefinition = def.getModelDefinition()
        val assetsManager = managers.assetsManager
        assetsManager.getCachedBoundingBox(modelDefinition)
        val gameModelInstance = GameModelInstance(
            ModelInstance(assetsManager.getAssetByDefinition(modelDefinition)),
            definition = modelDefinition
        )
        val entity = EntityBuilder.begin()
            .addModelInstanceComponent(
                gameModelInstance,
                position,
                true,
                direction.toFloat(),
            )
            .addAmbComponent(
                auxVector1.set(randomScale, randomScale, randomScale),
                if (def.isRandomizeRotation()) random(0F, 360F) else 0F,
                def
            )
            .finishAndAddToEngine()
        val physicsComponent = addPhysicsToAmbObject(modelDefinition, entity, gameModelInstance)
        addTurret(
            def, physicsComponent
        )
    }

    private fun addPhysicsToAmbObject(
        modelDefinition: ModelDefinition,
        entity: Entity,
        gameModelInstance: GameModelInstance
    ) = EntityBuilder.addPhysicsComponent(
        createShapeForAmbObject(modelDefinition),
        entity,
        managers.dispatcher,
        Matrix4(gameModelInstance.modelInstance.transform),
        0F,
        btCollisionObject.CollisionFlags.CF_STATIC_OBJECT
    )

    private fun addTurret(
        def: AmbDefinition,
        physicsComponent: PhysicsComponent
    ) {
        if (def == AmbDefinition.TURRET_CANNON) {
            val assetsManager = managers.assetsManager
            val modelInstance =
                ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.TURRET_CANNON))
            val entity = EntityBuilder.begin()
                .addEnemyComponent()
                .addModelInstanceComponent(
                    GameModelInstance(modelInstance, ModelDefinition.TURRET_CANNON),
                    physicsComponent.rigidBody.worldTransform.getTranslation(
                        auxVector1
                    ).add(0F, assetsManager.getCachedBoundingBox(ModelDefinition.TURRET_BASE).height, 0F),
                    true,
                )
                .finishAndAddToEngine()
            val cachedBoundingBox = assetsManager.getCachedBoundingBox(ModelDefinition.TURRET_CANNON)
            EntityBuilder.addPhysicsComponent(
                btBoxShape(
                    auxVector1.set(
                        cachedBoundingBox.width / 2F,
                        cachedBoundingBox.height / 2F,
                        cachedBoundingBox.depth / 2F
                    )
                ),
                entity,
                managers.dispatcher,
                modelInstance.transform,
                10F,
                btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT
            )
        }
    }

    private fun createShapeForAmbObject(modelDefinition: ModelDefinition): btCompoundShape {
        val shape = btCompoundShape()
        val dimensions = auxBoundingBox.set(managers.assetsManager.getCachedBoundingBox(modelDefinition)).getDimensions(
            auxVector3
        )
        val btBoxShape = btBoxShape(
            dimensions.scl(0.5F)
        )
        shape.addChildShape(
            auxMatrix.idt().translate(0F, dimensions.y / 2F, 0F), btBoxShape
        )
        return shape
    }

    companion object {
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxVector3 = Vector3()
        private const val MIN_SCALE = 0.95F
        private const val MAX_SCALE = 1.05F
        private const val EXT_SIZE = 48
        private val TILES_CHARS = CharArray(80) { (it + 48).toChar() }.joinToString("")
        private val auxBoundingBox = BoundingBox()
        private val auxMatrix = Matrix4()
    }
}
