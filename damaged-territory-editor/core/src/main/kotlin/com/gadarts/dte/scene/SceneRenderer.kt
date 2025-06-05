package com.gadarts.dte.scene

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Disposable
import com.gadarts.dte.scene.handlers.SceneRendererHandlers
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.SharedUtils

class SceneRenderer(assetsManager: GameAssetManager) : Table(), Disposable {
    private val auxiliaryModels = AuxiliaryModels(MAP_SIZE)
    private val floorModel = createFloorModel()
    private val tiles by lazy {
        Array(MAP_SIZE) {
            Array(MAP_SIZE) {
                val texture =
                    assetsManager.getTexture("tile_water")
                texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                val modelInstance = ModelInstance(floorModel)
                (modelInstance.materials.get(0)
                    .get(TextureAttribute.Diffuse) as TextureAttribute)
                    .set(TextureRegion(texture))
                modelInstance
            }
        }
    }
    private val handlers = SceneRendererHandlers(auxiliaryModels, assetsManager)

    private fun createFloorModel(): Model {
        val builder = ModelBuilder()
        builder.begin()
        SharedUtils.createFlatMesh(builder, "floor", 0.5F, null, 0F)
        return builder.end()
    }

    init {
        handlers.initialize(tiles)
        for (z in 0 until MAP_SIZE) {
            for (x in 0 until MAP_SIZE) {
                tiles[z][x].transform.setToTranslation(x.toFloat() + 0.5F, 0F, z.toFloat() + 0.5F)
            }
        }
    }

    fun render() {
        handlers.cameraHandler.update()
        val screenPosition = localToScreenCoordinates(auxVector2.set(0F, 0F))
        handlers.renderingHandler.render(screenPosition)
    }


    override fun dispose() {
        handlers.dispose()
        auxiliaryModels.dispose()
        floorModel.dispose()
    }


    companion object {
        private val auxVector2 = com.badlogic.gdx.math.Vector2()
        const val MAP_SIZE = 32
    }
}
