package com.gadarts.dte.scene.handlers.render

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.glutils.ShaderProgram

class GrayShader(private val shaderProgram: ShaderProgram) : Shader {

    private var dummyGrayTexture: Texture
    private lateinit var context: RenderContext
    private lateinit var camera: Camera

    init {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(0.5f, 0.5f, 0.5f, 1f) // Gray
        pixmap.fill()
        dummyGrayTexture = Texture(pixmap)
        pixmap.dispose()
    }

    override fun init() {
    }

    override fun begin(camera: Camera, context: RenderContext) {
        this.camera = camera
        this.context = context

        context.setDepthTest(GL20.GL_LEQUAL, 0f, 1f)
        context.setDepthMask(true)  // Keep depth writing enabled since we're discarding transparent pixels
        context.setBlending(true, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        shaderProgram.bind()
        shaderProgram.setUniformMatrix("u_projViewTrans", camera.combined)
        shaderProgram.setUniformf("u_grayFactor", 0.75f)
    }

    override fun render(renderable: Renderable) {
        val texAttr = renderable.material.get(TextureAttribute.Diffuse) as? TextureAttribute

        val unit = if (texAttr != null) {
            context.textureBinder.bind(texAttr.textureDescription)
        } else {
            context.textureBinder.bind(dummyGrayTexture)
        }

        shaderProgram.setUniformi("u_texture", unit)
        shaderProgram.setUniformMatrix("u_worldTrans", renderable.worldTransform)
        renderable.meshPart.render(shaderProgram)
    }

    override fun end() {
    }

    override fun dispose() {
        shaderProgram.dispose()
        dummyGrayTexture.dispose()
    }

    override fun canRender(instance: Renderable?): Boolean = true

    override fun compareTo(other: Shader?): Int = 0
}