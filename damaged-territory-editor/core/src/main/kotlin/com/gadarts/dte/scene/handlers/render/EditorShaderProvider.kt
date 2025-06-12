package com.gadarts.dte.scene.handlers.render

import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider

class EditorShaderProvider(
    private val grayShader: GrayShader
) : ShaderProvider {

    private val defaultShaderProvider = DefaultShaderProvider()

    override fun getShader(renderable: Renderable): Shader {
        val instance = renderable.userData as? EditorModelInstance
        val shader = if (instance?.applyGray == true) grayShader
        else defaultShaderProvider.getShader(renderable)

        // Ensure proper depth testing
        return shader
    }

    override fun dispose() {
        defaultShaderProvider.dispose()
        grayShader.dispose()
    }
}