package com.gadarts.returnfire.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.MathUtils.random
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.GeneralUtils
import com.gadarts.returnfire.Services
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.SfxDefinitions
import com.gadarts.returnfire.assets.TexturesDefinitions
import com.gadarts.returnfire.components.AmbComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.model.AmbModelDefinitions
import com.gadarts.returnfire.model.CharactersDefinitions
import com.gadarts.returnfire.model.GameMap

class MapSystem : GameEntitySystem() {

    private val ambSounds = listOf(
        SfxDefinitions.AMB_EAGLE,
        SfxDefinitions.AMB_WIND,
        SfxDefinitions.AMB_OUD
    )
    private var nextAmbSound: Long = TimeUtils.millis() + random(
        AMB_SND_INTERVAL_MIN,
        AMB_SND_INTERVAL_MAX
    )
    private lateinit var floors: Array<Array<Entity?>>
    private lateinit var ambEntities: ImmutableArray<Entity>
    private lateinit var floorModel: Model
    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = emptyMap()


    override fun initialize(gameSessionData: GameSessionData, services: Services) {
        super.initialize(gameSessionData, services)
        val builder = ModelBuilder()
        createFloorModel(builder)
        val tilesMapping = gameSessionData.currentMap.tilesMapping
        floors = Array(tilesMapping.size) { arrayOfNulls(tilesMapping[0].size) }
        gameSessionData.modelCache = ModelCache()
        addBackgroundSea()
        gameSessionData.currentMap.placedElements.forEach {
            if (it.definition != CharactersDefinitions.PLAYER) {
                addAmbModelObject(
                    services.assetsManager,
                    auxVector2.set(it.col.toFloat(), 0.01F, it.row.toFloat()),
                    it.definition as AmbModelDefinitions
                )
            }
        }
        applyTransformOnAmbEntities()
        initializeAmbObjectsBoundingBox()
    }

    private fun initializeAmbObjectsBoundingBox() {
        for (entity in ambEntities) {
            if (ComponentsMapper.modelInstance.has(entity)) {
                val boxCollisionComponent = ComponentsMapper.boxCollision.get(entity)
                boxCollisionComponent.getBoundingBox(auxBoundingBox)
                auxBoundingBox.mul(ComponentsMapper.modelInstance.get(entity).modelInstance.transform)
                boxCollisionComponent.setBoundingBox(auxBoundingBox)
            }
        }
    }

    override fun resume(delta: Long) {
        nextAmbSound += delta
    }

    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        val now = TimeUtils.millis()
        if (nextAmbSound < now) {
            nextAmbSound = now + random(AMB_SND_INTERVAL_MIN, AMB_SND_INTERVAL_MAX)
            services.soundPlayer.play(services.assetsManager.getAssetByDefinition(ambSounds.random()))
        }
    }

    private fun createFloorModel(builder: ModelBuilder) {
        builder.begin()
        val texture = services.assetsManager.getAssetByDefinition(TexturesDefinitions.TILE_WATER)
        GeneralUtils.createFlatMesh(builder, "floor", 0.5F, texture, 0F)
        floorModel = builder.end()
    }

    private fun addBackgroundSea() {
        gameSessionData.modelCache.begin()
        val tilesMapping = gameSessionData.currentMap.tilesMapping
        val depth = tilesMapping.size
        val width = tilesMapping[0].size
        addSeaRegion(depth, width)
        addAllExternalSea(width, depth)
        gameSessionData.modelCache.end()
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
        val modelInstance = ModelInstance(floorModel)
        createAndAddGroundTileEntity(
            modelInstance,
            auxVector1.set(x, 0F, z)
        )
        modelInstance.transform.scl(width.toFloat(), 1F, depth.toFloat())
        val textureAttribute =
            modelInstance.materials.first().get(TextureAttribute.Diffuse) as TextureAttribute
        initializeExternalSeaTextureAttribute(textureAttribute, width, depth)
        gameSessionData.modelCache.add(modelInstance)
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

    private fun addSeaRegion(
        rows: Int,
        cols: Int,
    ) {
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                addSeaTile(
                    row,
                    col,
                    ModelInstance(floorModel)
                )
            }
        }
    }

    private fun addSeaTile(
        row: Int,
        col: Int,
        modelInstance: ModelInstance
    ) {
        val entity = createAndAddGroundTileEntity(
            modelInstance,
            auxVector1.set(col.toFloat() + 0.5F, 0F, row.toFloat() + 0.5F)
        )
        gameSessionData.modelCache.add(modelInstance)
        var current = GameMap.TILE_TYPE_EMPTY
        if (row >= 0
            && col >= 0
            && row < gameSessionData.currentMap.tilesMapping.size
            && col < gameSessionData.currentMap.tilesMapping[0].size
        ) {
            current = gameSessionData.currentMap.tilesMapping[row][col]
            floors[row][col] = entity
        }
        val textureAttribute =
            modelInstance.materials.get(0).get(TextureAttribute.Diffuse) as TextureAttribute
        val texture = services.assetsManager.getAssetByDefinition(beachTiles[current.code - '0'.code])
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        textureAttribute.set(TextureRegion(texture))
    }

    private fun createAndAddGroundTileEntity(
        modelInstance: ModelInstance,
        position: Vector3
    ): Entity {
        return EntityBuilder.begin()
            .addModelInstanceComponent(modelInstance, position)
            .addGroundComponent()
            .finishAndAddToEngine()
    }

    private fun applyTransformOnAmbEntities() {
        ambEntities = engine.getEntitiesFor(Family.all(AmbComponent::class.java).get())
        ambEntities.forEach {
            if (!ComponentsMapper.modelInstance.has(it)) return
            val scale = ComponentsMapper.amb.get(it).getScale(auxVector1)
            val transform = ComponentsMapper.modelInstance.get(it).modelInstance.transform
            transform.scl(scale).rotate(Vector3.Y, ComponentsMapper.amb.get(it).rotation)
        }
    }

    private fun addAmbModelObject(
        am: GameAssetManager,
        position: Vector3,
        def: AmbModelDefinitions,
    ) {
        val randomScale = if (def.isRandomizeScale()) random(MIN_SCALE, MAX_SCALE) else 1F
        val scale = auxVector1.set(randomScale, randomScale, randomScale)
        val model = am.getAssetByDefinition(def.getModelDefinition())
        EntityBuilder.begin()
            .addModelInstanceComponent(model, position)
            .addAmbComponent(scale, if (def.isRandomizeRotation()) random(0F, 360F) else 0F)
            .addBoxCollisionComponent(model)
            .finishAndAddToEngine()
    }

    override fun dispose() {
        floorModel.dispose()
        gameSessionData.modelCache.dispose()
    }

    companion object {
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxBoundingBox = BoundingBox()
        private const val MIN_SCALE = 0.95F
        private const val MAX_SCALE = 1.05F
        private const val EXT_SIZE = 48
        private const val AMB_SND_INTERVAL_MIN = 7000
        private const val AMB_SND_INTERVAL_MAX = 22000
        private val beachTiles = listOf(
            TexturesDefinitions.TILE_WATER,
            TexturesDefinitions.TILE_BEACH_BOTTOM_RIGHT,
            TexturesDefinitions.TILE_BEACH_BOTTOM,
            TexturesDefinitions.TILE_BEACH_BOTTOM_LEFT,
            TexturesDefinitions.TILE_BEACH_RIGHT,
            TexturesDefinitions.TILE_BEACH_LEFT,
            TexturesDefinitions.TILE_BEACH_TOP_RIGHT,
            TexturesDefinitions.TILE_BEACH_TOP,
            TexturesDefinitions.TILE_BEACH_TOP_LEFT,
            TexturesDefinitions.TILE_BEACH,
        )

    }
}
