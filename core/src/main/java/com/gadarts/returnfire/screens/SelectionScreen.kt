package com.gadarts.returnfire.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.Screen
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.gadarts.returnfire.GeneralUtils
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.console.ConsoleImpl
import com.gadarts.returnfire.systems.render.AxisModelHandler

class SelectionScreen(
    private val assetsManager: GameAssetManager,
    dispatcher: MessageDispatcher,
) : Screen {
    private val sceneModelInstance by lazy { ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.SCENE)) }
    private val console = ConsoleImpl(assetsManager, dispatcher)
    private val batch by lazy { ModelBatch() }
    private val camera by lazy { GeneralUtils.createCamera() }

    private val debugInput: CameraInputController by lazy { CameraInputController(camera) }
    private var axisModelHandler = AxisModelHandler()

    override fun show() {
        debugInput.autoUpdate = true
        Gdx.input.inputProcessor = debugInput
        camera.position.set(0F, 8F, 8F)
        camera.lookAt(0F, 0F, 0F)
    }


    override fun render(delta: Float) {
        GeneralUtils.clearScreen()
        camera.update()
        debugInput.update()
        batch.begin(camera)
        axisModelHandler.render(batch)
        batch.render(sceneModelInstance)
        batch.end()

        if (Gdx.input.isKeyPressed(Input.Keys.BACK)) {
            Gdx.app.exit()
        }
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

}
