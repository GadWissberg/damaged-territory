package com.gadarts.returnfire.assets.loaders

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.files.FileHandle
import com.gadarts.returnfire.GameException
import com.gadarts.returnfire.model.AmbDefinition
import com.gadarts.returnfire.model.ElementDefinition
import com.gadarts.returnfire.model.ElementType
import com.gadarts.returnfire.model.GameMap
import com.gadarts.returnfire.model.PlacedElement
import com.gadarts.returnfire.screens.GamePlayScreen
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.util.Locale

class MapLoader(resolver: FileHandleResolver) :
    AsynchronousAssetLoader<GameMap, MapLoader.MapLoaderParameter>(resolver) {
    override fun getDependencies(
        fileName: String?,
        file: FileHandle?,
        parameter: MapLoaderParameter?
    ): com.badlogic.gdx.utils.Array<AssetDescriptor<Any>> {
        return com.badlogic.gdx.utils.Array()
    }

    override fun loadAsync(
        manager: AssetManager?,
        fileName: String?,
        file: FileHandle?,
        parameter: MapLoaderParameter?
    ) {
    }

    override fun loadSync(
        manager: AssetManager?,
        fileName: String?,
        file: FileHandle?,
        parameter: MapLoaderParameter?
    ): GameMap {
        val jsonObj = gson.fromJson(file!!.reader(), JsonObject::class.java)
        val tilesString = jsonObj.getAsJsonPrimitive(KEY_TILES_MAPPING).asString
        val size = jsonObj.getAsJsonPrimitive(KEY_SIZE).asInt
        if (size <= 0) {
            throw GameException("Failed loading map - size is 0! Map content: $jsonObj")
        }
        val tilesMapping = Array(size) { CharArray(size) }
        for (row in 0 until size) {
            for (col in 0 until size) {
                tilesMapping[row][col] = tilesString[row * size + col]
            }
        }
        return GameMap(tilesMapping, inflateElements(jsonObj))
    }

    private fun inflateElements(jsonObj: JsonObject): List<PlacedElement> {
        return jsonObj.getAsJsonArray(KEY_ELEMENTS)
            .filterNot { it.asJsonObject.get(KEY_DEFINITION).asString.equals("PLAYER") }.map {
                val asJsonObject = it.asJsonObject
                val definitionName = asJsonObject.get(KEY_DEFINITION).asString
                val allDefinitions = ElementType.entries.flatMap { elementType -> elementType.definitions.toList() }
                var definition: ElementDefinition? = null
                try {
                    definition = allDefinitions.find { def -> def.getName() == definitionName }
                } catch (e: IllegalArgumentException) {
                    try {
                        definition = AmbDefinition.valueOf(definitionName.uppercase(Locale.ROOT))
                    } catch (ignored: IllegalArgumentException) {
                    }
                }
                val row = asJsonObject.get(KEY_ROW).asInt
                val col = asJsonObject.get(KEY_COL).asInt
                val direction = asJsonObject.get(KEY_DIRECTION).asInt
                if (definition == null) {
                    Gdx.app.log(
                        GamePlayScreen::class.java.simpleName,
                        "Failed to find definition for element: $definitionName"
                    )
                }
                PlacedElement(definition!!, row, col, direction)
            }
    }

    companion object {
        private val gson = Gson()
        private const val KEY_TILES_MAPPING = "tiles"
        private const val KEY_ELEMENTS = "elements"
        private const val KEY_DEFINITION = "definition"
        private const val KEY_ROW = "row"
        private const val KEY_COL = "col"
        private const val KEY_DIRECTION = "direction"
        private const val KEY_SIZE = "size"
    }

    class MapLoaderParameter : AssetLoaderParameters<GameMap>()
}
