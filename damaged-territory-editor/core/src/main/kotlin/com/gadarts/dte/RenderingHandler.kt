package com.gadarts.dte

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Disposable

class RenderingHandler(
    private val auxiliaryModels: AuxiliaryModels,
    private val cameraHandler: CameraHandler,
) : Disposable {
    private lateinit var tiles: Array<Array<ModelInstance>>
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
        for (row in tiles) {
            for (tile in row) {
                modelsBatch.render(tile)
            }
        }
        modelsBatch.end()
    }

    override fun dispose() {
        shapeRenderer.dispose()
    }

    fun initialize(tiles: Array<Array<ModelInstance>>) {
        this.tiles = tiles
    }

}
