package com.gadarts.returnfire.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.Screen
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.GeneralUtils
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.console.ConsoleImpl
import com.gadarts.returnfire.systems.render.AxisModelHandler


class SelectionScreen(
    private val assetsManager: GameAssetManager,
    dispatcher: MessageDispatcher,
) : Screen {
    private var swingTime: Float = 0.0f
    private val sceneModelInstance by lazy { ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.SCENE)) }
    private val hookModelInstance by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.HOOK))
        modelInstance.transform.setToTranslation(Vector3(-1.5F, 5.2F, 4.3F))
        modelInstance
    }
    private val fanModelInstance by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.FAN))
        modelInstance.transform.setToTranslation(Vector3(0.9F, 0.1F, 4.4F))
        modelInstance
    }
    private val console = ConsoleImpl(assetsManager, dispatcher)
    private val batch by lazy { ModelBatch() }
    private val camera by lazy { GeneralUtils.createCamera() }

    private val debugInput: CameraInputController by lazy { CameraInputController(camera) }
    private var axisModelHandler = AxisModelHandler()

    override fun show() {
        debugInput.autoUpdate = true
        Gdx.input.inputProcessor = debugInput
        camera.position.set(0F, 8F, 15F)
        camera.lookAt(0F, 0F, 0F)
    }


    override fun render(delta: Float) {
        animateHook(delta)
        fanModelInstance.transform.rotate(Vector3.Y, 320F * delta)
        GeneralUtils.clearScreen()
        camera.update()
        debugInput.update()
        batch.begin(camera)
        axisModelHandler.render(batch)
        batch.render(sceneModelInstance)
        batch.render(hookModelInstance)
        batch.render(fanModelInstance)
        batch.end()

        if (Gdx.input.isKeyPressed(Input.Keys.BACK)) {
            Gdx.app.exit()
        }
    }

    private fun animateHook(delta: Float) {
        swingTime += delta * 0.5F
        val progress = (MathUtils.sin(swingTime * MathUtils.PI) + 1) / 2
        val swingAngleZ: Float = Interpolation.fade.apply(-0.03F, 0.03F, progress)
        val swingAngleY: Float = Interpolation.fade.apply(-0.2F, 0.2F, progress)
        val originalTransform: Matrix4 = hookModelInstance.transform
        hookModelInstance.transform.set(
            auxMatrix.idt()
                .set(originalTransform)
                .rotate(0f, 0f, 1f, swingAngleZ)
                .rotate(0F, 1F, 0F, swingAngleY)
        )
    }


    override fun resize(width: Int, height: Int) {
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun hide() {
        val inputMultiplexer = Gdx.input.inputProcessor as InputMultiplexer
        inputMultiplexer.removeProcessor(console)
    }

    override fun dispose() {
        batch.dispose()
    }

    companion object {
        private val auxMatrix = Matrix4()
    }
}
