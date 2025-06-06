package com.gadarts.dte.scene.handlers

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Plane
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.Ray
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.gadarts.shared.SharedUtils

class CursorHandler(private val sharedData: SceneRendererHandlersSharedData) : InputProcessor, SceneHandler {
    private var cursorModelInstance: ModelInstance
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
        cursorModelInstance = ModelInstance(cursorModel)
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
        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }

    override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return false
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        auxRay.set(sharedData.camera.getPickRay(screenX.toFloat(), screenY.toFloat()))

        val success = Intersector.intersectRayPlane(
            auxRay,
            floorPlane,
            auxVector
        )

        if (success) {
            val snappedX = MathUtils.floor(auxVector.x)
            val snappedZ = MathUtils.floor(auxVector.z)

            cursorModelInstance.transform.setToTranslation(snappedX.toFloat() + 0.5F, 0.01f, snappedZ.toFloat() + 0.5F)
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
    }
}
