package com.gadarts.shared

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.model.Node
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader
import com.badlogic.gdx.graphics.g3d.particles.batches.BillboardParticleBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.Array
import com.gadarts.shared.assets.AssetsTypes
import com.gadarts.shared.assets.definitions.AssetDefinition
import com.gadarts.shared.assets.definitions.ParticleEffectDefinition
import com.gadarts.shared.assets.definitions.external.ExternalDefinitions
import com.gadarts.shared.assets.definitions.external.TextureDefinition
import com.gadarts.shared.assets.definitions.model.ModelDefinition
import com.gadarts.shared.assets.loaders.DefinitionLoader
import com.gadarts.shared.assets.loaders.MapLoader
import com.gadarts.shared.assets.map.GameMap
import com.gadarts.shared.assets.utils.CollisionShapeInfo
import com.gadarts.shared.assets.utils.ModelCollisionShapeInfo
import com.gadarts.shared.data.GameModelInstanceInfo
import java.io.File
import java.util.*

open class GameAssetManager : AssetManager() {

    fun loadAssets() {
        initializeCustomLoaders()
        loadAllAssets()
        finishLoading()
        loadTextures()
        finishLoading()
        generateModelsBoundingBoxes()
        inflateCollisionShapesInfo()
    }

    private fun inflateCollisionShapesInfo() {
        Arrays.stream(ModelDefinition.entries.toTypedArray())
            .forEach { def ->
                val models: List<Model> = getAllAssetsByDefinition(def)
                models.forEachIndexed { index, model ->
                    val collisionShapes = inflateCollisionShapesInfo(model)
                    if (collisionShapes.isNotEmpty()) {
                        val modelCollisionShapeInfo = ModelCollisionShapeInfo(collisionShapes)
                        addAsset(
                            "$PREFIX_COLLISION_SHAPE${def.getDefinitionName()}${if (models.size > 1) "_$index" else ""}",
                            ModelCollisionShapeInfo::class.java,
                            modelCollisionShapeInfo
                        )
                    }
                    removeCollisionNodes(model.nodes)
                }
            }
    }

    private fun inflateCollisionShapesInfo(model: Model): MutableList<CollisionShapeInfo> {
        val collisionShapes = mutableListOf<CollisionShapeInfo>()
        for (node in model.nodes) {
            if (node.id.startsWith(PREFIX_COLLISION_SHAPE)) {
                val boundingBox = BoundingBox()
                node.calculateTransforms(true)
                node.calculateBoundingBox(boundingBox, true)
                val dimensions = Vector3()
                boundingBox.getDimensions(dimensions)
                val center = Vector3()
                boundingBox.getCenter(center)
                val collisionShapeInfo = CollisionShapeInfo(
                    dimensions,
                    center,
                )
                collisionShapes.add(collisionShapeInfo)
            }
        }
        return collisionShapes
    }

    private fun removeCollisionNodes(nodes: Array<Node>) {
        val iterator = nodes.iterator()
        while (iterator.hasNext()) {
            val node = iterator.next()
            if (node.id.startsWith(PREFIX_COLLISION_SHAPE, true)) {
                iterator.remove()
            }
        }
    }

    private fun loadTextures() {
        getTexturesDefinitions().definitions.forEach {
            val textureDefinition = it.value
            val fileName =
                "${TextureDefinition.FOLDER}${File.separator}${textureDefinition.folder}${File.separator}${textureDefinition.fileName}"
            if (textureDefinition.frames == 1) {
                load(
                    "$fileName.${TextureDefinition.FORMAT}",
                    Texture::class.java
                )
            } else {
                for (i in 0 until textureDefinition.frames) {
                    load(
                        "${fileName}_$i.${TextureDefinition.FORMAT}",
                        Texture::class.java
                    )
                }
            }
        }
    }

    private fun loadAllAssets() {
        AssetsTypes.entries.forEach { type ->
            if (!type.skipAutoLoad) {
                if (type.loadedUsingLoader) {
                    if (type.assets.isNotEmpty()) {
                        type.assets.forEach { asset ->
                            if (asset.getParameters() != null) {
                                loadFont(asset)
                            } else {
                                asset.getPaths().forEach {
                                    load(it, asset.getClazz())
                                }
                                if (type == AssetsTypes.MODELS) {
                                    val modelDefinition = asset as ModelDefinition
                                    modelDefinition.shadowsPaths.forEach {
                                        load(it, asset.getClazz())
                                    }
                                }
                            }
                        }
                    } else {
                        load("definitions/textures.json", ExternalDefinitions::class.java)
                    }
                } else {
                    loadTextualAsset(type)
                }
            }
        }
    }

    private fun loadTextualAsset(type: AssetsTypes) {
        type.assets.forEach { asset ->
            asset.getPaths().forEach {
                val content = Gdx.files.internal(it).readString()
                addAsset(
                    it,
                    String::class.java,
                    content
                )
            }
        }
    }

    private fun loadFont(asset: AssetDefinition<*>) {
        load(
            asset.getPaths().first(),
            BitmapFont::class.java,
            (asset.getParameters() as FreetypeFontLoader.FreeTypeFontLoaderParameter)
        )
    }


    fun getTexturesDefinitions(): ExternalDefinitions<TextureDefinition> {
        return get("definitions/${AssetsTypes.TEXTURES.name.lowercase()}.${ExternalDefinitions.FORMAT}")
    }

    private fun initializeCustomLoaders() {
        val resolver: FileHandleResolver = InternalFileHandleResolver()
        setLoader(FreeTypeFontGenerator::class.java, FreeTypeFontGeneratorLoader(resolver))
        val loader = FreetypeFontLoader(resolver)
        setLoader(BitmapFont::class.java, "ttf", loader)
        val mapLoader = MapLoader(resolver)
        setLoader(GameMap::class.java, "json", mapLoader)
        val definitionLoader = DefinitionLoader(resolver)
        setLoader(ExternalDefinitions::class.java, "json", definitionLoader)
    }

    fun getCachedBoundingBox(definition: ModelDefinition): BoundingBox {
        return auxBoundingBox.set(
            get(
                PREFIX_ASSET_BOUNDING_BOX + definition.getDefinitionName(),
                BoundingBox::class.java
            )
        )
    }

    fun getCachedModelCollisionShapeInfo(gameModelInstanceInfo: GameModelInstanceInfo): ModelCollisionShapeInfo? {
        val modelIndex = gameModelInstanceInfo.getModelIndex()
        val fileName =
            "$PREFIX_COLLISION_SHAPE${
                gameModelInstanceInfo.getDefinition()!!.getDefinitionName()
            }${if (modelIndex != null) "_$modelIndex" else ""}"
        val clazz = ModelCollisionShapeInfo::class.java
        val modelCollisionShapeInfo = get(fileName, clazz, false)
        return modelCollisionShapeInfo
    }

    fun loadParticleEffects(pointSpriteParticleBatch: BillboardParticleBatch) {
        ParticleEffectDefinition.entries.forEach {
            it.getPaths().forEach { _ ->
                load(
                    it.getPaths()[0],
                    ParticleEffect::class.java,
                    ParticleEffectLoader.ParticleEffectLoadParameter(
                        Array.with(
                            pointSpriteParticleBatch
                        )
                    )
                )
            }
        }
        finishLoading()
    }

    fun unloadParticleEffects() {
        ParticleEffectDefinition.entries.forEach {
            it.getPaths().forEach { _ ->
                unload(
                    it.getPaths()[0],
                )
            }
        }
        finishLoading()
    }

    private fun generateModelsBoundingBoxes() {
        Arrays.stream(ModelDefinition.entries.toTypedArray())
            .forEach { def ->
                val definitionName = def.getDefinitionName()
                val model: Model = getAssetByDefinition(def)
                val boundingBox = model.calculateBoundingBox(BoundingBox())
                if (!def.boundingBoxData.boundingBoxScale.epsilonEquals(Vector3(1F, 1F, 1F))) {
                    boundingBox.mul(auxMatrix.idt().scl(def.boundingBoxData.boundingBoxScale))
                }
                addAsset(
                    PREFIX_ASSET_BOUNDING_BOX + definitionName,
                    BoundingBox::class.java,
                    boundingBox
                )
            }
    }

    inline fun <reified T> getAssetByDefinition(definition: AssetDefinition<T>, index: Int = -1): T {
        val paths = definition.getPaths()
        return get(paths[if (index == -1) MathUtils.random(paths.size - 1) else index], T::class.java)
    }

    inline fun <reified T> getAllAssetsByDefinition(definition: AssetDefinition<T>): List<T> {
        return definition.getPaths().map { get(it, T::class.java) }
    }

    fun getTexture(name: String): Texture {
        val definition = getTexturesDefinitions().definitions[name]!!
        return getTexture(definition)
    }

    fun getTexture(definition: TextureDefinition, frameIndex: Int = 0): Texture {
        val path =
            "${AssetsTypes.TEXTURES.name.lowercase()}${File.separator}${definition.folder}${File.separator}${definition.fileName}"
        val fileName = if (definition.frames == 1) {
            "$path.${AssetsTypes.TEXTURES.format}"
        } else {
            "${path}_${frameIndex}.${AssetsTypes.TEXTURES.format}"
        }
        return get(
            fileName,
            Texture::class.java
        )
    }

    companion object {
        private val auxBoundingBox = BoundingBox()
        private val auxMatrix = Matrix4()
        private const val PREFIX_ASSET_BOUNDING_BOX = "box_"
        private const val PREFIX_COLLISION_SHAPE = "collision_shape_"
    }
}
