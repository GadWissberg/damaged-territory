package com.gadarts.returnfire.managers

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
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader
import com.badlogic.gdx.graphics.g3d.particles.batches.BillboardParticleBatch
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.Array
import com.gadarts.returnfire.assets.AssetsTypes
import com.gadarts.returnfire.assets.definitions.AssetDefinition
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.assets.definitions.external.ExternalDefinitions
import com.gadarts.returnfire.assets.definitions.external.TextureDefinition
import com.gadarts.returnfire.assets.loaders.DefinitionsLoader
import com.gadarts.returnfire.assets.loaders.MapLoader
import com.gadarts.returnfire.model.GameMap
import java.io.File
import java.util.*

open class GameAssetManager : AssetManager() {

    fun loadAssets() {
        initializeCustomLoaders()
        loadAllAssets()
        finishLoading()
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
        finishLoading()
        generateModelsBoundingBoxes()
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
        val definitionsLoader = DefinitionsLoader(resolver)
        setLoader(ExternalDefinitions::class.java, "json", definitionsLoader)
    }

    fun getCachedBoundingBox(definition: ModelDefinition): BoundingBox {
        return auxBoundingBox.set(
            get(
                BOUNDING_BOX_PREFIX + definition.getDefinitionName(),
                BoundingBox::class.java
            )
        )
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
                if (!def.boundingBoxScale.epsilonEquals(Vector3(1F, 1F, 1F))) {
                    boundingBox.mul(auxMatrix.idt().scl(def.boundingBoxScale))
                }
                addAsset(
                    BOUNDING_BOX_PREFIX + definitionName,
                    BoundingBox::class.java,
                    boundingBox
                )
            }
    }

    inline fun <reified T> getAssetByDefinition(definition: AssetDefinition<T>, index: Int = -1): T {
        val paths = definition.getPaths()
        val selectedPath = if (index == -1) paths.random() else paths[index]
        return get(selectedPath, T::class.java)
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
        private const val BOUNDING_BOX_PREFIX = "box_"
    }
}
