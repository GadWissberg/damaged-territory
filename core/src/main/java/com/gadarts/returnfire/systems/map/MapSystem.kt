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
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.MathUtils.random
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.physics.bullet.collision.btBoxShape
import com.badlogic.gdx.physics.bullet.collision.btCompoundShape
import com.gadarts.returnfire.GeneralUtils
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.assets.definitions.external.TextureDefinition
import com.gadarts.returnfire.components.AmbComponent
import com.gadarts.returnfire.components.AnimatedTextureComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.GroundComponent
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.model.AmbDefinition
import com.gadarts.returnfire.model.CharactersDefinitions
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents

class MapSystem : GameEntitySystem() {

    private val ambSoundsHandler = AmbSoundsHandler()
    private var groundTextureAnimationStateTime = 0F
    private lateinit var animatedFloorsEntities: ImmutableArray<Entity>
    private lateinit var floors: Array<Array<Entity?>>
    private lateinit var ambEntities: ImmutableArray<Entity>
    private lateinit var floorModel: Model

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = emptyMap()

    override fun initialize(gameSessionData: GameSessionData, managers: Managers) {
        super.initialize(gameSessionData, managers)
        animatedFloorsEntities =
            engine.getEntitiesFor(Family.all(GroundComponent::class.java, AnimatedTextureComponent::class.java).get())
        val builder = ModelBuilder()
        createFloorModel(builder)
        val tilesMapping = gameSessionData.currentMap.tilesMapping
        floors = Array(tilesMapping.size) { arrayOfNulls(tilesMapping[0].size) }
        gameSessionData.gameSessionDataRender.modelCache = ModelCache()
        addFloor()
    }

    override fun onSystemReady() {
        super.onSystemReady()
        addAmbEntities(gameSessionData)
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

    private fun addFlag(position: Vector3) {
        EntityBuilder.begin()
            .addAmbComponent(auxVector2.set(0.5F, 0.5F, 0.5F), 0F, AmbDefinition.FLAG)
            .addModelInstanceComponent(
                GameModelInstance(
                    ModelInstance(
                        managers.assetsManager.getAssetByDefinition(
                            ModelDefinition.FLAG
                        )
                    ),
                    definition = ModelDefinition.FLAG
                ),
                position,
                false
            )
            .finishAndAddToEngine()
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
    }


    override fun dispose() {
        floorModel.dispose()
        gameSessionData.gameSessionDataRender.modelCache.dispose()
    }

    private fun createFloorModel(builder: ModelBuilder) {
        builder.begin()
        val texture =
            managers.assetsManager.getTexture("tile_water")
        GeneralUtils.createFlatMesh(builder, "floor", 0.5F, texture, 0F)
        floorModel = builder.end()
    }

    private fun addFloor() {
        gameSessionData.gameSessionDataRender.modelCache.begin()
        val tilesMapping = gameSessionData.currentMap.tilesMapping
        val depth = tilesMapping.size
        val width = tilesMapping[0].size
        addFloorRegion(depth, width)
        addAllExternalSea(width, depth)
        gameSessionData.gameSessionDataRender.modelCache.end()
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
        val modelInstance = GameModelInstance(ModelInstance(floorModel), definition = null)
        val entity = createAndAddGroundTileEntity(
            modelInstance,
            auxVector1.set(x, 0F, z)
        )
        modelInstance.modelInstance.transform.scl(width.toFloat(), 1F, depth.toFloat())
        val textureAttribute =
            modelInstance.modelInstance.materials.first()
                .get(TextureAttribute.Diffuse) as TextureAttribute
        initializeExternalSeaTextureAttribute(textureAttribute, width, depth)
        gameSessionData.gameSessionDataRender.modelCache.add(modelInstance.modelInstance)
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
                        ModelInstance(floorModel),
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
        gameSessionData.gameSessionDataRender.modelCache.add(modelInstance.modelInstance)
        var textureDefinition: TextureDefinition? = null
        val playerPosition =
            ComponentsMapper.modelInstance.get(gameSessionData.player).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector1
            )
        val texturesDefinitions =
            managers.assetsManager.getTexturesDefinitions()
        if (playerPosition.x.toInt() == col && playerPosition.z.toInt() == row) {
            textureDefinition = texturesDefinitions.definitions["base_door"]
        } else if (row >= 0
            && col >= 0
            && row < gameSessionData.currentMap.tilesMapping.size
            && col < gameSessionData.currentMap.tilesMapping[0].size
        ) {
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
        }
    }

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
        ambEntities = engine.getEntitiesFor(Family.all(AmbComponent::class.java).get())
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
        val scale = auxVector1.set(randomScale, randomScale, randomScale)
        val modelDefinition = def.getModelDefinition()
        val model = managers.assetsManager.getAssetByDefinition(modelDefinition)
        val modelInstance = ModelInstance(model)
        managers.assetsManager.getCachedBoundingBox(modelDefinition)
        val shape = btCompoundShape()
        val dimensions = auxBoundingBox.set(managers.assetsManager.getCachedBoundingBox(modelDefinition)).getDimensions(
            auxVector3
        )
        shape.addChildShape(
            auxMatrix.idt().translate(0F, dimensions.y / 2F, 0F), btBoxShape(
                dimensions.scl(0.5F)
            )
        )
        val gameModelInstance = GameModelInstance(modelInstance, definition = modelDefinition)
        val entity = EntityBuilder.begin()
            .addModelInstanceComponent(
                gameModelInstance,
                position,
                true,
                direction.toFloat()
            )
            .addAmbComponent(scale, if (def.isRandomizeRotation()) random(0F, 360F) else 0F, def)
            .finishAndAddToEngine()
        EntityBuilder.addPhysicsComponent(
            shape,
            entity,
            managers.dispatcher,
            Matrix4(gameModelInstance.modelInstance.transform),
            0F
        )
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
