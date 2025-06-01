package com.gadarts.shared.assets.definitions

import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.audio.Music

enum class MusicDefinition : AssetDefinition<Music> {

    TEST;

    private val paths = ArrayList<String>()

    init {
        initializePaths("music/%s.ogg", getPaths())
    }

    override fun getPaths(): ArrayList<String> {
        return paths
    }

    override fun getParameters(): AssetLoaderParameters<Music>? {
        return null
    }

    override fun getClazz(): Class<Music> {
        return Music::class.java
    }

    override fun getDefinitionName(): String {
        return name
    }
}
