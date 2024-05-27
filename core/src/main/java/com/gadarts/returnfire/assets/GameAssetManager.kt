package com.gadarts.returnfire.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.math.collision.BoundingBox
import com.gadarts.returnfire.model.GameMap
import java.io.File.separatorChar
import java.util.Arrays
import java.util.Locale

open class GameAssetManager : AssetManager() {

    fun loadAssets(assetsFolderPath: String) {
        initializeCustomLoaders()
        AssetsTypes.entries.forEach { type ->
            if (type.isLoadedUsingLoader()) {
                if (type.assets.isNotEmpty()) {
                    type.assets.forEach { asset ->
                        if (asset.getParameters() != null) {
                            load(
                                asset.getPaths().first(),
                                BitmapFont::class.java,
                                (asset.getParameters() as FreetypeFontLoader.FreeTypeFontLoaderParameter)
                            )
                        } else {
                            asset.getPaths().forEach { load(it, asset.getClazz()) }
                        }
                    }
                } else {
                    val toLowerCase = type.name.lowercase(Locale.ROOT)
                    val path = "$assetsFolderPath$toLowerCase"
                    val dir = Gdx.files.internal(path)
                    dir.list().forEach {
                        load(
                            "$path$separatorChar${it.name()}",
                            GameMap::class.java
                        )
                    }
                }
            } else {
                type.assets.forEach { asset ->
                    asset.getPaths().forEach {
                        addAsset(
                            it,
                            String::class.java,
                            Gdx.files.internal(it).readString()
                        )
                    }
                }
            }
        }
        finishLoading()
        generateModelsBoundingBoxes()
    }

    private fun initializeCustomLoaders() {
        val resolver: FileHandleResolver = InternalFileHandleResolver()
        setLoader(FreeTypeFontGenerator::class.java, FreeTypeFontGeneratorLoader(resolver))
        val loader = FreetypeFontLoader(resolver)
        setLoader(BitmapFont::class.java, "ttf", loader)
        val mapLoader = MapLoader(resolver)
        setLoader(GameMap::class.java, "json", mapLoader)
    }

    fun getCachedBoundingBox(definition: ModelDefinition): BoundingBox {
        return auxBoundingBox.set(
            get(
                BOUNDING_BOX_PREFIX + definition.getDefinitionName(),
                BoundingBox::class.java
            )
        )
    }

    private fun generateModelsBoundingBoxes() {
        Arrays.stream(ModelDefinition.entries.toTypedArray())
            .forEach { def ->
                val definitionName = def.getDefinitionName()
                val model: Model = getAssetByDefinition(def)
                addAsset(
                    BOUNDING_BOX_PREFIX + definitionName,
                    BoundingBox::class.java,
                    model.calculateBoundingBox(BoundingBox())
                )
            }
    }

    inline fun <reified T> getAssetByDefinition(definition: AssetDefinition<T>): T {
        return get(definition.getPaths().random(), T::class.java)
    }

    inline fun <reified T> getAssetByDefinitionAndIndex(definition: AssetDefinition<T>, i: Int): T {
        return get(definition.getPaths()[i], T::class.java)
    }

    companion object {
        private val auxBoundingBox = BoundingBox()
        private const val BOUNDING_BOX_PREFIX = "box_"
    }
}
