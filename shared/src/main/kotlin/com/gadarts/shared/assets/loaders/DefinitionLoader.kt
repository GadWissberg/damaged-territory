package com.gadarts.shared.assets.loaders

import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import com.gadarts.shared.assets.definitions.external.ExternalDefinitions
import com.gadarts.shared.assets.definitions.external.TextureDefinition
import com.google.gson.Gson
import com.google.gson.JsonObject

class DefinitionLoader(resolver: FileHandleResolver) :
    AsynchronousAssetLoader<ExternalDefinitions<*>, AssetLoaderParameters<ExternalDefinitions<*>>>(
        resolver
    ) {
    override fun getDependencies(
        fileName: String?,
        file: FileHandle?,
        parameter: AssetLoaderParameters<ExternalDefinitions<*>>?
    ): Array<AssetDescriptor<Any>> {
        return Array()
    }

    override fun loadAsync(
        manager: AssetManager?,
        fileName: String?,
        file: FileHandle?,
        parameter: AssetLoaderParameters<ExternalDefinitions<*>>?
    ) {
    }

    override fun loadSync(
        manager: AssetManager?,
        fileName: String?,
        file: FileHandle?,
        parameter: AssetLoaderParameters<ExternalDefinitions<*>>?
    ): ExternalDefinitions<Any> {
        val jsonObj = gson.fromJson(file!!.reader(), JsonObject::class.java)
        val definitions = jsonObj.getAsJsonArray(KEY_DEFINITIONS)
            .associateBy({ definition ->
                definition.asJsonObject.get(KEY_FILE_NAME).asString
            }, { definition ->
                val asJsonObject = definition.asJsonObject
                TextureDefinition(
                    asJsonObject.get(KEY_FILE_NAME).asString,
                    if (asJsonObject.has(KEY_FRAMES)) asJsonObject.get(KEY_FRAMES).asInt else 1,
                    if (asJsonObject.has(KEY_ANIMATED)) asJsonObject.get(KEY_ANIMATED).asBoolean else false,
                    if (asJsonObject.has(KEY_FOLDER)) asJsonObject.get(KEY_FOLDER).asString else "",
                    if (asJsonObject.has(KEY_SURROUNDED_TILE)) asJsonObject.get(KEY_SURROUNDED_TILE).asBoolean else false,
                )
            })
        return ExternalDefinitions(definitions)
    }


    companion object {
        private val gson = Gson()
        private const val KEY_DEFINITIONS = "definitions"
        private const val KEY_FILE_NAME = "file_name"
        private const val KEY_FOLDER = "folder"
        private const val KEY_FRAMES = "frames"
        private const val KEY_ANIMATED = "animated"
        private const val KEY_SURROUNDED_TILE = "surrounded_tile"
    }

}
