package com.gadarts.returnfire.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.Screen
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
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.definitions.FontDefinition
import com.gadarts.returnfire.model.CharacterDefinition
import com.gadarts.returnfire.model.SimpleCharacterDefinition
import com.gadarts.returnfire.model.TurretCharacterDefinition

class SelectionScreen(
    private val assetsManager: GameAssetManager,
    private val screensManager: ScreensManager,
    private val runsOnMobile: Boolean
) : Screen {
    private val stage by lazy { Stage() }

    override fun show() {
        val table = Table()
        table.setFillParent(true)
        table.add(
            createLabel("Damaged Territory - 0.6", assetsManager.getAssetByDefinition(FontDefinition.WOK_STENCIL))
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
        stage.addActor(table)
        table.debug(if (GameDebugSettings.UI_DEBUG) Table.Debug.all else Table.Debug.none)
        (Gdx.input.inputProcessor as InputMultiplexer).addProcessor(stage)
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
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun hide() {
        (Gdx.input.inputProcessor as InputMultiplexer).removeProcessor(stage)
    }

    override fun dispose() {
        stage.dispose()
    }

    companion object {
        private const val LABEL_PADDING = 20F
    }
}
