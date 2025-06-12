package com.gadarts.dte.scene.handlers

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.*
import com.badlogic.gdx.math.collision.Ray
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.gadarts.dte.TileLayer
import com.gadarts.dte.scene.SceneRenderer.Companion.MAP_SIZE
import com.gadarts.dte.scene.SharedData
import com.gadarts.dte.scene.handlers.render.EditorModelInstance
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.SharedUtils

class CursorHandler(
    private val sharedData: SharedData,
    private val assetsManager: GameAssetManager,
    dispatcher: MessageDispatcher
) : InputProcessor,
    SceneHandler(dispatcher) {
    private val prevTileClickPosition = Vector2()
    private var tiling: Boolean = false
    private var deleting: Boolean = false
    private var cursorModelInstance: EditorModelInstance
    private var cursorModel: Model
    private var cursorMaterial: Material

    init {
        val modelBuilder = ModelBuilder()
        modelBuilder.begin()
        cursorMaterial = Material(
            ColorAttribute.createDiffuse(Color.GREEN),
        )
        SharedUtils.createFlatMesh(modelBuilder, "cursor", 0.5F, null, 0F, cursorMaterial)
        cursorModel = modelBuilder.end()
        cursorModelInstance = EditorModelInstance(cursorModel)
        sharedData.modelInstances.add(cursorModelInstance)
        (Gdx.input.inputProcessor as InputMultiplexer).addProcessor(this)
    }


    override fun keyDown(keycode: Int): Boolean {
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        return false
    }

    override fun keyTyped(character: Char): Boolean {
        return false
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val cursorPosition = cursorModelInstance.transform.getTranslation(auxVector)
        val z = cursorPosition.z.toInt()
        val x = cursorPosition.x.toInt()
        var appliedAction = false
        if (button == Input.Buttons.LEFT) {
            appliedAction = placeTile(x, z)
            if (appliedAction) {
                tiling = true
            }
        } else if (button == Input.Buttons.RIGHT) {
            appliedAction = deleteTile(x, z)
            if (appliedAction) {
                deleting = true
            }
        }
        if (appliedAction) {
            prevTileClickPosition.set(x.toFloat(), z.toFloat())
        }
        return appliedAction
    }

    private fun deleteTile(x: Int, z: Int): Boolean {
        val tileLayer = sharedData.layers[sharedData.selectedLayerIndex]
        if (tileLayer.tiles[z][x] != null) {
            tileLayer.tiles[z][x]?.let { modelInstance ->
                sharedData.modelInstances.remove(modelInstance)
                tileLayer.tiles[z][x] = null
            }
            tileLayer.bitMap[z][x] = 0
            return true
        }
        return false
    }

    private fun placeTile(
        x: Int,
        z: Int,
    ): Boolean {
        val tileLayer = sharedData.layers[sharedData.selectedLayerIndex]
        val selectedTile = sharedData.selectedTile
        if (selectedTile != null) {
            prevTileClickPosition.set(x.toFloat(), z.toFloat())
            addTile(tileLayer, z, x, selectedTile.fileName.lowercase())
            tileLayer.bitMap[z][x] = 1
            if (selectedTile.surroundedTile) {
                applyTileSurrounding(x - 1, z - 1, tileLayer)
                applyTileSurrounding(x, z - 1, tileLayer)
                applyTileSurrounding(x + 1, z - 1, tileLayer)
                applyTileSurrounding(x - 1, z, tileLayer)
                applyTileSurrounding(x + 1, z, tileLayer)
                applyTileSurrounding(x - 1, z + 1, tileLayer)
                applyTileSurrounding(x, z + 1, tileLayer)
                applyTileSurrounding(x + 1, z + 1, tileLayer)
            }
            return true
        }
        return false
    }

    private fun addTile(
        tileLayer: TileLayer,
        z: Int,
        x: Int,
        textureName: String
    ): ModelInstance {
        val tiles = tileLayer.tiles
        val modelInstance = if (
            tiles[z][x] != null
        ) {
            tiles[z][x]!!
        } else {
            val modelInstance = EditorModelInstance(sharedData.floorModel)
            sharedData.modelInstances.add(modelInstance)
            modelInstance
        }
        (modelInstance.materials[0].get(
            TextureAttribute.Diffuse
        ) as TextureAttribute
                ).textureDescription.texture = assetsManager.getTexture(textureName)
        tileLayer.tiles[z][x] =
            modelInstance
        modelInstance.transform.setToTranslation(
            x.toFloat() + 0.5F,
            sharedData.layers.indexOf(tileLayer).toFloat() * 0.01F,
            z.toFloat() + 0.5F
        )
        return modelInstance
    }

    private fun applyTileSurrounding(x: Int, z: Int, tileLayer: TileLayer) {
        if (sharedData.selectedTile == null || x < 0 || x >= MAP_SIZE || z < 0 || z >= MAP_SIZE || tileLayer.disabled) return

        val tileSignature = SharedUtils.calculateTileSignature(x, z, tileLayer.bitMap)
        val textureSignature = signatures.keys
            .sortedByDescending { it.countOneBits() }
            .find { (it and tileSignature) == it }
        if (textureSignature != null) {
            if (textureSignature > 0) {
                val textureName =
                    "${sharedData.selectedTile!!.fileName.lowercase()}${signatures[textureSignature]}"
                addTile(
                    tileLayer,
                    z,
                    x,
                    textureName
                )
            }
        }
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        tiling = false
        deleting = false
        return true
    }

    override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        updateCursorPosition(screenX, screenY)
        val cursorPosition = cursorModelInstance.transform.getTranslation(auxVector)
        val snappedX = MathUtils.floor(cursorPosition.x)
        val snappedZ = MathUtils.floor(cursorPosition.z)
        if (!prevTileClickPosition.epsilonEquals(snappedX.toFloat(), snappedZ.toFloat())) {
            if (tiling) {
                placeTile(snappedX, snappedZ)
                prevTileClickPosition.set(snappedX.toFloat(), snappedZ.toFloat())
                return true
            } else if (deleting) {
                deleteTile(snappedX, snappedZ)
                prevTileClickPosition.set(snappedX.toFloat(), snappedZ.toFloat())
                return true
            }
        }
        return false
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return updateCursorPosition(screenX, screenY)
    }

    private fun updateCursorPosition(screenX: Int, screenY: Int): Boolean {
        auxRay.set(sharedData.camera.getPickRay(screenX.toFloat(), screenY.toFloat()))

        val success = Intersector.intersectRayPlane(auxRay, floorPlane, auxVector)

        if (success) {
            val snappedX = MathUtils.floor(auxVector.x)
            val snappedZ = MathUtils.floor(auxVector.z)


            cursorModelInstance.transform.setToTranslation(
                MathUtils.clamp(snappedX.toFloat() + 0.5F, 0.5F, MAP_SIZE.toFloat() - 0.5F),
                0.07f,
                MathUtils.clamp(snappedZ.toFloat() + 0.5F, 0.5F, MAP_SIZE.toFloat() - 0.5F),
            )
            return true
        }

        return false
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {

        return false
    }

    override fun update(parent: Table, deltaTime: Float) {
    }

    override fun dispose() {
        cursorModel.dispose()
    }

    companion object {
        private val auxRay = Ray()
        private val auxVector = Vector3()
        private val floorPlane = Plane(Vector3.Y, 0f)
        private val signatures = mapOf(
            0b00000010 to "_top",
            0b01000000 to "_bottom",
            0b00001000 to "_left",
            0b00010000 to "_right",
            0b00010110 to "_gulf_top_right",
            0b00001011 to "_gulf_top_left",
            0b11010000 to "_gulf_bottom_right",
            0b01101000 to "_gulf_bottom_left",
            0b10000000 to "_bottom_right",
            0b00100000 to "_bottom_left",
            0b00000100 to "_top_right",
            0b00000001 to "_top_left",
            0b11111111 to "",
        )

    }
}
