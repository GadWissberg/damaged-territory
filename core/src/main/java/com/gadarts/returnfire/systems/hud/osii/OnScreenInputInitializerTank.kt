package com.gadarts.returnfire.systems.hud.osii

import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.systems.hud.HudButtons
import com.gadarts.returnfire.systems.hud.HudSystem

class OnScreenInputInitializerTank(
    private val hudSystem: HudSystem,
    private val hudButtons: HudButtons,
    private val turretTouchpad: Touchpad
) :
        (Table, Cell<Touchpad>) -> Unit {
    override fun invoke(ui: Table, movementPad: Cell<Touchpad>) {
        val imageButtonCell = hudSystem.addButton(
            ui,
            "icon_reverse",
            hudButtons.reverseButtonClickListener
        )
        imageButtonCell.grow().left().bottom().padBottom(32F)
        val attackButtonsTable = Table()
        attackButtonsTable.setDebug(GameDebugSettings.UI_DEBUG, true)
        hudSystem.addButton(
            attackButtonsTable,
            "icon_missiles",
            hudButtons.secWeaponButtonClickListener,
        ).center().row()
        ui.add(attackButtonsTable).right()
        hudSystem.addTouchpad(ui, turretTouchpad).pad(0F, 0F, 0F, HudSystem.JOYSTICK_PADDING).top()
    }

}