package com.gadarts.returnfire.assets.loaders

import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import com.gadarts.returnfire.assets.definitions.ExternalDefinition
import com.gadarts.returnfire.assets.definitions.TextureDefinition
import com.gadarts.returnfire.assets.definitions.TexturesDefinitions
import com.gadarts.returnfire.assets.loaders.DefinitionsLoader.DefinitionsLoaderParameter
import com.google.gson.Gson
import com.google.gson.JsonObject

class DefinitionsLoader(resolver: FileHandleResolver) :
    AsynchronousAssetLoader<ExternalDefinition, DefinitionsLoaderParameter>(resolver) {
    override fun getDependencies(
        fileName: String?,
        file: FileHandle?,
        parameter: DefinitionsLoaderParameter?
    ): Array<AssetDescriptor<Any>> {
        return Array()
    }

    override fun loadAsync(
        manager: AssetManager?,
        fileName: String?,
        file: FileHandle?,
        parameter: DefinitionsLoaderParameter?
    ) {
    }

    override fun loadSync(
        manager: AssetManager?,
        fileName: String?,
        file: FileHandle?,
        parameter: DefinitionsLoaderParameter?
    ): ExternalDefinition {
        val jsonObj = gson.fromJson(file!!.reader(), JsonObject::class.java)
        val definitions = jsonObj.getAsJsonArray(KEY_DEFINITIONS)
            .map { definition ->
                val asJsonObject = definition.asJsonObject
                TextureDefinition(
                    asJsonObject.get(KEY_FILE_NAME).asString,
                    if (asJsonObject.has(KEY_FRAMES)) asJsonObject.get(KEY_FRAMES).asInt else 1,
                    if (asJsonObject.has(KEY_ANIMATED)) asJsonObject.get(KEY_ANIMATED).asBoolean else false,
                )
            }
        return TexturesDefinitions(definitions)
    }

    class DefinitionsLoaderParameter : AssetLoaderParameters<ExternalDefinition>()

    companion object {
        private val gson = Gson()
        private const val KEY_DEFINITIONS = "definitions"
        private const val KEY_FILE_NAME = "file_name"
        private const val KEY_FRAMES = "frames"
        private const val KEY_ANIMATED = "animated"
    }

}
