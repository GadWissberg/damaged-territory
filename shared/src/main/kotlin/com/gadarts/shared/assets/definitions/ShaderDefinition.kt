package com.gadarts.shared.assets.definitions

import com.badlogic.gdx.assets.AssetLoaderParameters

enum class ShaderDefinition : AssetDefinition<String> {
    ;

    private val paths = ArrayList<String>()

    init {
        initializePaths("shaders/%s.shader", output = getPaths())
    }

    override fun getPaths(): ArrayList<String> {
        return paths
    }

    override fun getParameters(): AssetLoaderParameters<String>? {
        return null
    }

    override fun getClazz(): Class<String> {
        return String::class.java
    }

    override fun getDefinitionName(): String {
        return name
    }
}
