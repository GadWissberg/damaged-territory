package com.gadarts.returnfire.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.Screen
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.ScreenUtils
import com.gadarts.returnfire.DamagedTerritory
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.definitions.FontDefinition
import com.gadarts.returnfire.console.ConsoleImpl
import com.gadarts.returnfire.model.CharacterDefinition
import com.gadarts.returnfire.model.SimpleCharacterDefinition
import com.gadarts.returnfire.model.TurretCharacterDefinition

class SelectionScreen(
    private val assetsManager: GameAssetManager,
    private val screensManager: ScreensManager,
    private val runsOnMobile: Boolean,
    dispatcher: MessageDispatcher,
) : Screen {
    private val console = ConsoleImpl(assetsManager, dispatcher)
    private val stage by lazy {
        Stage()
    }

    override fun show() {
        stage.addActor(console)
        val table = Table()
        table.add(
            createLabel(
                "Damaged Territory - ${DamagedTerritory.VERSION}",
                assetsManager.getAssetByDefinition(FontDefinition.WOK_STENCIL)
            )
        ).pad(LABEL_PADDING).left().top().row()
        table.add(
            createLabel("Select Vehicle:", assetsManager.getAssetByDefinition(FontDefinition.WOK_STENCIL))
        ).pad(LABEL_PADDING).left().row()
        addVehicleLine(
            table,
            "Apache",
            SimpleCharacterDefinition.APACHE,
            "Arrows to move, primary attack - left ctrl, secondary attack - left shift"
        )
        addVehicleLine(
            table,
            "Tank",
            TurretCharacterDefinition.TANK,
            "Arrows to move, primary attack - left ctrl, rotate turret - A and D"
        )
        val scaleX = Gdx.graphics.width / 1920f
        val scaleY = Gdx.graphics.height / 1080f
        val scaleFactor = minOf(scaleX, scaleY) // Use the smaller scale factor for both dimensions
        table.isTransform = true // Enable transformations
        table.setScale(scaleFactor) // Apply scaling
        stage.addActor(table)
        table.debug(if (GameDebugSettings.UI_DEBUG) Table.Debug.all else Table.Debug.none)
        (Gdx.input.inputProcessor as InputMultiplexer).addProcessor(stage)
        val viewport = stage.viewport
        table.setPosition(
            (viewport.worldWidth - table.width) / 2,
            (viewport.worldHeight - table.height) / 2
        )
        console.toFront()
    }

    private fun addVehicleLine(
        table: Table,
        text: String,
        definition: CharacterDefinition,
        inputText: String
    ) {
        table.add(
            createLabel(text, assetsManager.getAssetByDefinition(FontDefinition.WOK_STENCIL))
        )
        val button = Button(
            TextureRegionDrawable(assetsManager.getTexture("button_up")),
            TextureRegionDrawable(assetsManager.getTexture("button_down"))
        )
        table.add(
            button
        )
        if (!runsOnMobile) {
            table.add(createLabel(inputText, assetsManager.getAssetByDefinition(FontDefinition.WOK_STENCIL)))
        }
        table.row()
        button.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                super.clicked(event, x, y)
                screensManager.goToWarScreen(definition)
            }
        })
    }

    private fun createLabel(text: String, bitmapFont: BitmapFont) = Label(
        text,
        Label.LabelStyle(
            bitmapFont,
            Color.valueOf("#673b2b")
        )
    )

    override fun render(delta: Float) {
        ScreenUtils.clear(Color.BLACK)
        stage.act()
        stage.draw()
        if (Gdx.input.isKeyPressed(Input.Keys.BACK)) {
            Gdx.app.exit()
        }
    }


    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun hide() {
        val inputMultiplexer = Gdx.input.inputProcessor as InputMultiplexer
        inputMultiplexer.removeProcessor(stage)
        inputMultiplexer.removeProcessor(console)
    }

    override fun dispose() {
        stage.dispose()
    }

    companion object {
        private const val LABEL_PADDING = 20F
    }
}