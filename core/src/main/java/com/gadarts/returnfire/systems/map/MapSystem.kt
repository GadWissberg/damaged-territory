package com.gadarts.returnfire.systems.map

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
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
import com.gadarts.returnfire.components.arm.ArmProperties
import com.gadarts.returnfire.components.bullet.BulletBehavior
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.components.physics.PhysicsComponent
import com.gadarts.returnfire.model.*
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents

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
    private val ambEntities: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(
            Family.all(AmbComponent::class.java).get()
        )
    }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> =
        mapOf()


    override fun initialize(gameSessionData: GameSessionData, managers: Managers) {
        super.initialize(gameSessionData, managers)
        val tilesMapping = gameSessionData.currentMap.tilesMapping
        gameSessionData.tilesEntities = Array(tilesMapping.size) { arrayOfNulls(tilesMapping[0].size) }
        gameSessionData.floorModel = createFloorModel()
        gameSessionData.renderData.modelCache = ModelCache()
        addFloor()
    }


    override fun onSystemReady() {
        super.onSystemReady()
        addAmbEntities()
        addCharacters()
    }

    private fun addCharacters() {
        gameSessionData.currentMap.placedElements.filter {
            val definition = it.definition
            definition.getType() == ElementType.CHARACTER && definition != SimpleCharacterDefinition.PLAYER
        }
            .forEach {
                addCharacter(
                    auxVector2.set(it.col.toFloat() + 0.5F, 0.01F, it.row.toFloat() + 0.5F),
                    it.definition as CharacterDefinition,
                    it.direction
                )
            }
    }

    private fun addCharacter(position: Vector3, characterDefinition: CharacterDefinition, direction: Int) {
        val gameModelInstance = createGameModelInstance(characterDefinition.getModelDefinition())
        val baseEntity = EntityBuilder.begin()
            .addModelInstanceComponent(
                gameModelInstance,
                position,
                null,
                direction.toFloat(),
            )
            .addCharacterComponent(TurretCharacterDefinition.TURRET_CANNON)
            .addEnemyComponent()
            .finishAndAddToEngine()
        addPhysicsToStaticObject(baseEntity, gameModelInstance)
        if (characterDefinition.getCharacterType() == CharacterType.TURRET) {
            addTurret(baseEntity)
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
            (ComponentsMapper.modelInstance.get(entity).gameModelInstance.modelInstance.materials.get(0)
                .get(TextureAttribute.Diffuse) as TextureAttribute).textureDescription.texture =
                keyFrame
        }
        waterSplashEntitiesToRemove.clear()
        for (entity in waterSplashEntities) {
            val groundBlastComponent = ComponentsMapper.waterWave.get(entity)
            if (TimeUtils.timeSinceMillis(groundBlastComponent.creationTime) > groundBlastComponent.duration) {
                waterSplashEntitiesToRemove.add(entity)
            } else {
                val modelInstance = ComponentsMapper.modelInstance.get(entity).gameModelInstance.modelInstance
                modelInstance.transform.scl(groundBlastComponent.scalePace)
                val blendAttribute = modelInstance.materials.get(0).get(BlendingAttribute.Type) as BlendingAttribute
                blendAttribute.opacity -= groundBlastComponent.fadeOutPace
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

    private fun addAmbEntities() {
        gameSessionData.currentMap.placedElements.filter { it.definition.getType() == ElementType.AMB }.forEach {
            addAmbObject(
                auxVector2.set(it.col.toFloat() + 0.5F, 0.01F, it.row.toFloat() + 0.5F),
                it.definition as AmbDefinition,
                it.direction
            )
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
        val modelInstance = GameModelInstance(ModelInstance(gameSessionData.floorModel), null)
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
                        null,
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
            gameSessionData.tilesEntities[row][col] = entity
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
            .addModelInstanceComponent(modelInstance, position, null)
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
        val gameModelInstance = createGameModelInstance(def.getModelDefinition())
        val randomScale = if (def.isRandomizeScale()) random(MIN_SCALE, MAX_SCALE) else 1F
        val entity = EntityBuilder.begin()
            .addModelInstanceComponent(
                gameModelInstance,
                position,
                null,
                direction.toFloat(),
            )
            .addAmbComponent(
                auxVector1.set(randomScale, randomScale, randomScale),
                if (def.isRandomizeRotation()) random(0F, 360F) else 0F,
                def
            )
            .finishAndAddToEngine()
        addPhysicsToStaticObject(entity, gameModelInstance)
    }

    private fun createGameModelInstance(modelDefinition: ModelDefinition): GameModelInstance {
        val assetsManager = managers.assetsManager
        assetsManager.getCachedBoundingBox(modelDefinition)
        val gameModelInstance = GameModelInstance(
            ModelInstance(assetsManager.getAssetByDefinition(modelDefinition)),
            modelDefinition,
        )
        return gameModelInstance
    }

    private fun addPhysicsToStaticObject(
        entity: Entity,
        gameModelInstance: GameModelInstance
    ): PhysicsComponent {
        return EntityBuilder.addPhysicsComponent(
            createShapeForStaticObject(gameModelInstance.definition!!),
            entity,
            Matrix4(gameModelInstance.modelInstance.transform),
            0F,
            managers.dispatcher,
            btCollisionObject.CollisionFlags.CF_STATIC_OBJECT,
        )
    }

    private fun addTurret(
        baseEntity: Entity
    ) {
        val assetsManager = managers.assetsManager
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.TURRET_CANNON))
        val relativePositionCalculator = object : ArmComponent.RelativePositionCalculator {
            override fun calculate(parent: Entity, output: Vector3): Vector3 {
                return output.setZero()
                    .add(0.7F, 0F, ComponentsMapper.turret.get(parent).updateCurrentShootingArm() * 0.3F)
                    .rot(modelInstance.transform)
            }
        }
        val model = GameModelInstance(
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.CANNON_SPARK)),
            ModelDefinition.CANNON_SPARK,
        )
        val spark = EntityBuilder.begin()
            .addModelInstanceComponent(
                model,
                Vector3(),
                managers.assetsManager.getCachedBoundingBox(ModelDefinition.TURRET_CANNON),
                hidden = true
            )
            .addSparkComponent(relativePositionCalculator)
            .finishAndAddToEngine()
        val turret = EntityBuilder.begin()
            .addEnemyComponent()
            .addModelInstanceComponent(
                GameModelInstance(modelInstance, ModelDefinition.TURRET_CANNON),
                ComponentsMapper.physics.get(baseEntity).rigidBody.worldTransform.getTranslation(
                    auxVector1
                ).add(0F, assetsManager.getCachedBoundingBox(ModelDefinition.TURRET_BASE).height, 0F),
                null,
            )
            .addTurretComponent(baseEntity)
            .addPrimaryArmComponent(
                spark,
                ArmProperties(
                    5,
                    assetsManager.getAssetByDefinition(SoundDefinition.CANNON),
                    3000L,
                    20F,
                    0.5F,
                    null,
                    ModelDefinition.CANNON_BULLET,
                    null,
                    gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE_SMALL),
                    null,
                    true,
                    managers.assetsManager.getCachedBoundingBox(ModelDefinition.CANNON_BULLET)
                ),
                BulletBehavior.REGULAR
            )
            .finishAndAddToEngine()
        ComponentsMapper.character.get(baseEntity).child = turret
        val cachedBoundingBox = assetsManager.getCachedBoundingBox(ModelDefinition.TURRET_CANNON)
        val shape = btCompoundShape()
        shape.addChildShape(
            auxMatrix.idt().translate(ModelDefinition.TURRET_CANNON.boundingBoxBias), btBoxShape(
                auxVector1.set(
                    cachedBoundingBox.width / 2F,
                    cachedBoundingBox.height / 2F,
                    cachedBoundingBox.depth / 2F
                )
            )
        )
        EntityBuilder.addPhysicsComponent(
            shape,
            turret,
            modelInstance.transform,
            10F,
            managers.dispatcher,
            btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT,
        )

    }


    private fun createShapeForStaticObject(modelDefinition: ModelDefinition): btCompoundShape {
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
