package com.gadarts.returnfire.screens.hangar

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.gadarts.returnfire.DamagedTerritory
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.assets.definitions.FontDefinition
import com.gadarts.returnfire.managers.GameAssetManager
import com.gadarts.returnfire.model.definitions.SimpleCharacterDefinition
import com.gadarts.returnfire.model.definitions.TurretCharacterDefinition

class HangarScreenMenu(
    private val runsOnMobile: Boolean,
    private val assetsManager: GameAssetManager,
    private val stage: Stage,
    private val hangarSceneHandler: HangarSceneHandler
) {
    fun init() {
        buttonsTable.add(tankButton)
        buttonsTable.add(apacheButton)
        buttonsTable.bottom()
        stage.addActor(buttonsTable)
        textTable.add(Image(assetsManager.getTexture("logo"))).expandX().left().row()
        textTable.pad(20F).add(
            Label(
                DamagedTerritory.VERSION,
                Label.LabelStyle(
                    assetsManager.getAssetByDefinition(FontDefinition.WOK_STENCIL),
                    Color.WHITE
                )
            )
        ).top().left().row()
        val desktopText =
            "Arrows - Movement,\nCTRL - Primary attack,\nSHIFT - Secondary attack/return to base,\n" +
                    "ALT + LEFT/RIGHT - Rotate turret/strafe,\nENTER - Switch ground/air aim,\n'~' - Open console"
        val androidText =
            "An on-screen game-pad will appear in-game"
        addDescription(if (runsOnMobile) androidText else desktopText)
        addDescription("Select aiming:")
        addAimingOptions()
        stage.addActor(textTable)
    }

    private fun createTable(): Table {
        val table = Table()
        table.setFillParent(true)
        table.debug(if (GameDebugSettings.UI_DEBUG) Table.Debug.all else Table.Debug.none)
        return table
    }

    private fun addAimingOptions() {
        val buttonsTable = Table()
        addAimingButton(AIM_BUTTON_AUTOAIM, aimButtonGroup, buttonsTable)
        addAimingButton(AIM_BUTTON_MANUAL, aimButtonGroup, buttonsTable)
        textTable.add(buttonsTable).expand().top().left()
        aimButtonGroup.setChecked(AIM_BUTTON_AUTOAIM)
        aimButtonGroup.setMaxCheckCount(1)
        aimButtonGroup.setMinCheckCount(1)
    }

    private fun addAimingButton(
        text: String,
        buttonGroup: ButtonGroup<TextButton>,
        buttonsTable: Table
    ): Cell<TextButton> {
        val button = TextButton(
            text,
            TextButton.TextButtonStyle(
                TextureRegionDrawable(assetsManager.getTexture("button_up")),
                TextureRegionDrawable(assetsManager.getTexture("button_down")),
                TextureRegionDrawable(assetsManager.getTexture("button_checked")),
                assetsManager.getAssetByDefinition(FontDefinition.WOK_STENCIL)
            )
        )
        val cell = buttonsTable.add(button).top().left().size(100F, 100F).pad(10F)
        buttonGroup.add(button)
        return cell
    }

    private fun addDescription(text: String) {
        textTable.add(
            Label(
                text,
                Label.LabelStyle(
                    assetsManager.getAssetByDefinition(FontDefinition.CONSOLA),
                    Color.WHITE
                )
            )
        ).expandX().top().left().row()
    }

    private fun onClick(text: String) = object : ClickListener() {
        override fun clicked(event: InputEvent?, x: Float, y: Float) {
            when (text) {
                "Tank" -> {
                    buttonsTable.remove()
                    hangarSceneHandler.selectCharacter(TurretCharacterDefinition.TANK)
                }

                "Apache" -> {
                    buttonsTable.remove()
                    hangarSceneHandler.selectCharacter(SimpleCharacterDefinition.APACHE)
                }
            }
        }
    }

    fun isAutoAimSelected(): Boolean {
        return aimButtonGroup.checked.text.toString() == AIM_BUTTON_AUTOAIM
    }

    fun show() {
        stage.addActor(buttonsTable)
    }

    private val aimButtonGroup: ButtonGroup<TextButton> = ButtonGroup()
    private val buttonsTable: Table by lazy {
        createTable()
    }
    private val textTable: Table by lazy {
        createTable()
    }

    private fun addVehicleButton(text: String): TextButton {
        val textButton = TextButton(
            text,
            TextButton.TextButtonStyle(
                TextureRegionDrawable(assetsManager.getTexture("button_up")),
                TextureRegionDrawable(assetsManager.getTexture("button_down")),
                null,
                assetsManager.getAssetByDefinition(FontDefinition.WOK_STENCIL)
            )
        )
        textButton.addListener(onClick(text))
        return textButton
    }

    private val tankButton: TextButton by lazy { addVehicleButton("Tank") }

    private val apacheButton: TextButton by lazy { addVehicleButton("Apache") }

    companion object {
        private const val AIM_BUTTON_AUTOAIM = "Auto-Aim"
        private const val AIM_BUTTON_MANUAL = "Manual"
    }
}
