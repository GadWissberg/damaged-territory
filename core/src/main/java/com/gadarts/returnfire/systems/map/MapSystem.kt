package com.gadarts.returnfire.systems.map

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.MathUtils.random
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.GeneralUtils
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.assets.definitions.TextureDefinition
import com.gadarts.returnfire.components.*
import com.gadarts.returnfire.model.AmbDefinition
import com.gadarts.returnfire.model.CharactersDefinitions
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.GameSessionData
import com.gadarts.returnfire.systems.GameSessionData.Companion.REGION_SIZE
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.events.data.EntityEnteredNewRegionEventData

class MapSystem : GameEntitySystem() {

    private lateinit var animatedFloorsEntities: ImmutableArray<Entity>
    private var groundTextureAnimationStateTime = 0F
    private val ambSounds = listOf(
        SoundDefinition.AMB_EAGLE,
        SoundDefinition.AMB_WIND,
        SoundDefinition.AMB_OUD
    )
    private var nextAmbSound: Long = TimeUtils.millis() + random(
        AMB_SND_INTERVAL_MIN,
        AMB_SND_INTERVAL_MAX
    )
    private lateinit var floors: Array<Array<Entity?>>
    private lateinit var ambEntities: ImmutableArray<Entity>
    private lateinit var floorModel: Model

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> =
        mapOf(SystemEvents.ENTITY_ENTERED_NEW_REGION to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                managers: Managers
            ) {
                moveObjectFromRegionToAnotherRegion(
                    EntityEnteredNewRegionEventData.newRow,
                    EntityEnteredNewRegionEventData.newColumn,
                    gameSessionData.player,
                    EntityEnteredNewRegionEventData.prevRow,
                    EntityEnteredNewRegionEventData.prevColumn,
                )
            }
        })

    override fun initialize(gameSessionData: GameSessionData, managers: Managers) {
        super.initialize(gameSessionData, managers)
        animatedFloorsEntities =
            engine.getEntitiesFor(Family.all(GroundComponent::class.java, AnimatedTextureComponent::class.java).get())
        addEntityListener(gameSessionData)
        val builder = ModelBuilder()
        createFloorModel(builder)
        val tilesMapping = gameSessionData.currentMap.tilesMapping
        gameSessionData.entitiesAcrossRegions =
            Array(tilesMapping.size / REGION_SIZE) { arrayOfNulls(tilesMapping[0].size / REGION_SIZE) }
        floors = Array(tilesMapping.size) { arrayOfNulls(tilesMapping[0].size) }
        gameSessionData.modelCache = ModelCache()
        addFloor()
        gameSessionData.currentMap.placedElements.forEach {
            if (it.definition != CharactersDefinitions.PLAYER) {
                addAmbModelObject(
                    auxVector2.set(it.col.toFloat() + 0.5F, 0.01F, it.row.toFloat() + 0.5F),
                    it.definition as AmbDefinition,
                    it.direction
                )
            }
        }
        applyTransformOnAmbEntities()
    }

    private fun addEntityListener(gameSessionData: GameSessionData) {
        engine.addEntityListener(object : EntityListener {
            override fun entityAdded(entity: Entity) {

            }

            override fun entityRemoved(entity: Entity) {
                if (ComponentsMapper.modelInstance.has(entity)) {
                    val position =
                        ComponentsMapper.modelInstance.get(entity).gameModelInstance.modelInstance.transform.getTranslation(
                            auxVector1
                        )
                    gameSessionData.entitiesAcrossRegions[position.z.toInt() / REGION_SIZE][position.x.toInt() / REGION_SIZE]?.remove(
                        entity
                    )
                    if (ComponentsMapper.amb.has(entity)) {
                        if (ComponentsMapper.amb.get(entity).definition == AmbDefinition.BUILDING_FLAG) {
                            addFlag(position)
                        }
                        managers.dispatcher.dispatchMessage(
                            SystemEvents.BUILDING_DESTROYED.ordinal,
                            entity
                        )
                    }
                }
            }

        })
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


    private fun moveObjectFromRegionToAnotherRegion(
        newRow: Int, newColumn: Int, entity: Entity, prevRow: Int = -1, prevColumn: Int = -1,
    ) {
        val maxRow = gameSessionData.currentMap.tilesMapping.size / REGION_SIZE
        val maxCol = gameSessionData.currentMap.tilesMapping[0].size / REGION_SIZE
        if ((prevRow == newRow && prevColumn == newColumn)
            || newRow >= maxRow
            || newColumn >= maxCol
        )
            return

        if (gameSessionData.entitiesAcrossRegions[newRow][newColumn] == null) {
            gameSessionData.entitiesAcrossRegions[newRow][newColumn] =
                mutableListOf()
        }
        if (prevRow >= 0 && prevColumn >= 0 && prevRow < maxRow && prevColumn < maxCol) {
            gameSessionData.entitiesAcrossRegions[prevRow][prevColumn]?.remove(
                entity
            )
        }
        gameSessionData.entitiesAcrossRegions[newRow][newColumn]?.add(
            entity
        )
    }

    override fun resume(delta: Long) {
        nextAmbSound += delta
    }

    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        val now = TimeUtils.millis()
        if (nextAmbSound < now) {
            nextAmbSound = now + random(AMB_SND_INTERVAL_MIN, AMB_SND_INTERVAL_MAX)
            managers.soundPlayer.play(managers.assetsManager.getAssetByDefinition(ambSounds.random()))
        }
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

    private fun createFloorModel(builder: ModelBuilder) {
        builder.begin()
        val texture = managers.assetsManager.getAssetByDefinition(TextureDefinition.TILE_WATER)
        GeneralUtils.createFlatMesh(builder, "floor", 0.5F, texture, 0F)
        floorModel = builder.end()
    }

    private fun addFloor() {
        gameSessionData.modelCache.begin()
        val tilesMapping = gameSessionData.currentMap.tilesMapping
        val depth = tilesMapping.size
        val width = tilesMapping[0].size
        addFloorRegion(depth, width)
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
        gameSessionData.modelCache.add(modelInstance.modelInstance)
        applyAnimatedTextureComponentToFloor(TextureDefinition.TILE_WATER, entity)
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
        gameSessionData.modelCache.add(modelInstance.modelInstance)
        var textureDefinition: TextureDefinition? = null
        val playerPosition =
            ComponentsMapper.modelInstance.get(gameSessionData.player).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector1
            )
        if (playerPosition.x.toInt() == row && playerPosition.z.toInt() == col) {
            textureDefinition = TextureDefinition.BASE_DOOR
        } else if (row >= 0
            && col >= 0
            && row < gameSessionData.currentMap.tilesMapping.size
            && col < gameSessionData.currentMap.tilesMapping[0].size
        ) {
            floors[row][col] = entity
            textureDefinition =
                beachTiles[BASE_64_CHARS.indexOfFirst { c: Char -> gameSessionData.currentMap.tilesMapping[row][col] == c }]
        }
        if (textureDefinition != null) {
            val textureAttribute =
                modelInstance.modelInstance.materials.get(0)
                    .get(TextureAttribute.Diffuse) as TextureAttribute
            val texture =
                managers.assetsManager.getAssetByDefinition(textureDefinition)
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            textureAttribute.set(TextureRegion(texture))
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
        for (i in 0 until textureDefinition.fileNames) {
            frames.add(managers.assetsManager.getAssetByDefinitionAndIndex(textureDefinition, i))
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

    private fun addAmbModelObject(
        position: Vector3,
        def: AmbDefinition,
        direction: Int,
    ) {
        val randomScale = if (def.isRandomizeScale()) random(MIN_SCALE, MAX_SCALE) else 1F
        val scale = auxVector1.set(randomScale, randomScale, randomScale)
        val modelDefinition = def.getModelDefinition()
        val model = managers.assetsManager.getAssetByDefinition(modelDefinition)
        val modelInstance = ModelInstance(model)
        val entity = EntityBuilder.begin()
            .addModelInstanceComponent(
                GameModelInstance(modelInstance, definition = modelDefinition),
                position,
                true,
                direction.toFloat()
            )
            .addAmbComponent(scale, if (def.isRandomizeRotation()) random(0F, 360F) else 0F, def)
            .finishAndAddToEngine()
        moveObjectFromRegionToAnotherRegion(
            position.z.toInt() / REGION_SIZE,
            position.x.toInt() / REGION_SIZE,
            entity
        )
    }

    override fun dispose() {
        floorModel.dispose()
        gameSessionData.modelCache.dispose()
    }

    companion object {
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private const val MIN_SCALE = 0.95F
        private const val MAX_SCALE = 1.05F
        private const val EXT_SIZE = 48
        private const val AMB_SND_INTERVAL_MIN = 7000
        private const val AMB_SND_INTERVAL_MAX = 22000
        private const val BASE_64_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz+/"
        private val beachTiles = listOf(
            TextureDefinition.TILE_WATER,
            TextureDefinition.TILE_BEACH_BOTTOM_RIGHT,
            TextureDefinition.TILE_BEACH_GULF_BOTTOM_RIGHT,
            TextureDefinition.TILE_BEACH_BOTTOM,
            TextureDefinition.TILE_BEACH_BOTTOM_LEFT,
            TextureDefinition.TILE_BEACH_GULF_BOTTOM_LEFT,
            TextureDefinition.TILE_BEACH_RIGHT,
            TextureDefinition.TILE_BEACH_LEFT,
            TextureDefinition.TILE_BEACH_TOP_RIGHT,
            TextureDefinition.TILE_BEACH_GULF_TOP_RIGHT,
            TextureDefinition.TILE_BEACH_TOP,
            TextureDefinition.TILE_BEACH_TOP_LEFT,
            TextureDefinition.TILE_BEACH_GULF_TOP_LEFT,
            TextureDefinition.TILE_BEACH,
            TextureDefinition.TILE_WATER_SHALLOW_BOTTOM_RIGHT,
            TextureDefinition.TILE_WATER_SHALLOW_GULF_BOTTOM_RIGHT,
            TextureDefinition.TILE_WATER_SHALLOW_BOTTOM,
            TextureDefinition.TILE_WATER_SHALLOW_BOTTOM_LEFT,
            TextureDefinition.TILE_WATER_SHALLOW_GULF_BOTTOM_LEFT,
            TextureDefinition.TILE_WATER_SHALLOW_RIGHT,
            TextureDefinition.TILE_WATER_SHALLOW_LEFT,
            TextureDefinition.TILE_WATER_SHALLOW_TOP_RIGHT,
            TextureDefinition.TILE_WATER_SHALLOW_GULF_TOP_RIGHT,
            TextureDefinition.TILE_WATER_SHALLOW_TOP,
            TextureDefinition.TILE_WATER_SHALLOW_TOP_LEFT,
            TextureDefinition.TILE_WATER_SHALLOW_GULF_TOP_LEFT,
            TextureDefinition.TILE_WATER_SHALLOW,
            TextureDefinition.TILE_BEACH_ROAD_HORIZONTAL,
            TextureDefinition.TILE_BEACH_ROAD_VERTICAL,
            TextureDefinition.TILE_BEACH_ROAD_BOTTOM_RIGHT,
            TextureDefinition.TILE_BEACH_ROAD_BOTTOM_LEFT,
            TextureDefinition.TILE_BEACH_ROAD_TOP_LEFT,
            TextureDefinition.TILE_BEACH_ROAD_TOP_RIGHT,
            TextureDefinition.TILE_BEACH_ROAD_CROSS,
        )

    }
}
