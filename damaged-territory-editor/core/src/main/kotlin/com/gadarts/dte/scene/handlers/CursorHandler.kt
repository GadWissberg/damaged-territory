package com.gadarts.dte.scene.handlers

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.*
import com.badlogic.gdx.math.collision.Ray
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.gadarts.dte.EditorEvents
import com.gadarts.dte.ObjectFactory
import com.gadarts.dte.TileFactory
import com.gadarts.dte.TileLayer
import com.gadarts.dte.scene.SceneRenderer.Companion.MAP_SIZE
import com.gadarts.dte.scene.SharedData
import com.gadarts.dte.scene.handlers.render.EditorModelInstance
import com.gadarts.dte.ui.Modes
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.SharedUtils

class CursorHandler(
    private val sharedData: SharedData,
    private val assetsManager: GameAssetManager,
    private val tilesFactory: TileFactory,
    private val objectFactory: ObjectFactory,
    dispatcher: MessageDispatcher,
) : InputProcessor,
    SceneHandler(dispatcher) {
    private val prevTileClickPosition = Vector2()
    private var placingElement: Boolean = false
    private var deletingElements: Boolean = false
    private var cursorModelInstance: EditorModelInstance? = null
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
        setCursorModelInstance(cursorModel)
        (Gdx.input.inputProcessor as InputMultiplexer).addProcessor(this)
    }

    private fun setCursorModelInstance(model: Model) {
        val modelInstances = sharedData.modelInstances
        modelInstances.remove(cursorModelInstance)
        val editorModelInstance = EditorModelInstance(model)
        cursorModelInstance = editorModelInstance
        modelInstances.add(editorModelInstance)
        editorModelInstance.nodes.forEach { node ->
            node.parts.forEach { nodePart ->
                nodePart.material = cursorMaterial
            }
        }
        editorModelInstance.materials.clear()
        editorModelInstance.materials.add(cursorMaterial)
    }

    override val subscribedEvents: Map<EditorEvents, EditorOnEvent> = mapOf(
        EditorEvents.OBJECT_SELECTED to object : EditorOnEvent {
            override fun react(msg: Telegram) {
                val selectedObject = sharedData.selectedObject ?: return

                setCursorModelInstance(assetsManager.getAssetByDefinition(selectedObject.getModelDefinition()))
            }
        })

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
        if (cursorModelInstance == null) return false

        val cursorPosition = cursorModelInstance!!.transform.getTranslation(auxVector)
        val z = cursorPosition.z.toInt()
        val x = cursorPosition.x.toInt()
        var appliedAction = false
        if (button == Input.Buttons.LEFT) {
            appliedAction = placeElement(x, z)
            if (appliedAction) {
                placingElement = true
            }
        } else if (button == Input.Buttons.RIGHT) {
            appliedAction = deleteElement(x, z)
            if (appliedAction) {
                deletingElements = true
            }
        }
        if (appliedAction) {
            prevTileClickPosition.set(x.toFloat(), z.toFloat())
        }
        return appliedAction
    }

    private fun deleteElement(x: Int, z: Int): Boolean {
        if (sharedData.selectedMode == Modes.TILES) {
            val tileLayer = sharedData.layers[sharedData.selectedLayerIndex]
            if (tileLayer.tiles[z][x] != null) {
                tileLayer.tiles[z][x]?.let { placedTile ->
                    sharedData.modelInstances.remove(placedTile.modelInstance)
                    tileLayer.tiles[z][x] = null
                }
                tileLayer.bitMap[z][x] = 0
                return true
            }
        } else if (sharedData.selectedMode == Modes.OBJECTS) {
            val modelInstances = sharedData.modelInstances
            sharedData.modelInstances.first {
                it.transform.getTranslation(auxVector).let { position ->
                    position.x.toInt() == x && position.z.toInt() == z
                }
            }.let { modelInstance ->
                modelInstances.remove(modelInstance)
                sharedData.placedObjects.removeIf { placedObject ->
                    placedObject.modelInstance == modelInstance
                }
                return true
            }
        }
        return false
    }

    private fun placeElement(
        x: Int,
        z: Int,
    ): Boolean {
        if (sharedData.selectedMode == Modes.TILES) {
            return placeTile(x, z)
        } else if (sharedData.selectedMode == Modes.OBJECTS) {
            val selectedObject = sharedData.selectedObject
            return if (selectedObject != null) objectFactory.addObject(
                x,
                z,
                selectedObject
            ) else false
        }
        return false
    }


    private fun placeTile(x: Int, z: Int): Boolean {
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
    ) {
        val tiles = tileLayer.tiles
        if (
            tiles[z][x] != null
        ) {
            tilesFactory.initializeTile(textureName, x, sharedData.layers.indexOf(tileLayer), z, tiles[z][x]!!)
        } else {
            tilesFactory.addTile(textureName, tileLayer, x, z)
        }

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
        placingElement = false
        deletingElements = false
        return true
    }

    override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (cursorModelInstance == null) return false

        updateCursorPosition(screenX, screenY)
        val cursorPosition = cursorModelInstance!!.transform.getTranslation(auxVector)
        val snappedX = MathUtils.floor(cursorPosition.x)
        val snappedZ = MathUtils.floor(cursorPosition.z)
        if (!prevTileClickPosition.epsilonEquals(snappedX.toFloat(), snappedZ.toFloat())) {
            if (placingElement) {
                placeElement(snappedX, snappedZ)
                prevTileClickPosition.set(snappedX.toFloat(), snappedZ.toFloat())
                return true
            } else if (deletingElements) {
                deleteElement(snappedX, snappedZ)
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
        if (cursorModelInstance == null) return false

        auxRay.set(sharedData.camera.getPickRay(screenX.toFloat(), screenY.toFloat()))

        val success = Intersector.intersectRayPlane(auxRay, floorPlane, auxVector)

        if (success) {
            val snappedX = MathUtils.floor(auxVector.x)
            val snappedZ = MathUtils.floor(auxVector.z)


            cursorModelInstance!!.transform.setToTranslation(
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
