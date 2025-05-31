package com.gadarts.dte

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Disposable

class RenderingHandler(
    private val auxiliaryModels: AuxiliaryModels,
    private val cameraHandler: CameraHandler
) : Disposable {
    private val modelsBatch = ModelBatch()
    private val shapeRenderer = ShapeRenderer()
    fun render(screenPosition: Vector2) {
        Gdx.gl.glViewport(
            screenPosition.x.toInt(),
            screenPosition.y.toInt(),
            Gdx.graphics.backBufferWidth,
            Gdx.graphics.backBufferHeight
        )
        modelsBatch.begin(cameraHandler.camera)
        auxiliaryModels.render(modelsBatch)
        modelsBatch.end()
    }


    private fun createEnvironment(): Environment {
        val environment = Environment()
        environment.set(ColorAttribute.createAmbientLight(0.5f, 0.5f, 0.5f, 1f))
        val directionalLight = DirectionalLight()
        directionalLight.set(Color.WHITE, 0.5f, -0.8f, -0.2f)
        environment.add(directionalLight)
        return environment
    }

    override fun dispose() {
        shapeRenderer.dispose()
    }

}
