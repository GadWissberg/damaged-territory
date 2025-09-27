package com.gadarts.returnfire.ecs.systems.hud.osii

import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad
import com.gadarts.returnfire.ecs.systems.hud.HudButtons
import com.gadarts.returnfire.ecs.systems.hud.HudSystem

class OnScreenInputInitializerApache(
    private val hudSystem: HudSystem,
    private val hudButtons: HudButtons,
    private val autoAim: Boolean
) :
        (Table, Cell<Touchpad>) -> Unit {
    override fun invoke(ui: Table, movementPad: Cell<Touchpad>) {
        hudSystem.addRadar(ui).pad(0F, RADAR_PADDING_HORIZONTAL, 0F, RADAR_PADDING_HORIZONTAL)
        movementPad.expandX().left()
        addApacheButtons(ui)
    }

    private fun addApacheButtons(ui: Table) {
        hudSystem.addButton(
            ui,
            "icon_bullets",
            clickListener = hudButtons.priWeaponButtonClickListener
        )
        hudSystem.addButton(
            ui,
            "icon_missiles",
            hudButtons.secWeaponButtonClickListener,
            HudSystem.JOYSTICK_PADDING_HORIZONTAL,
        )
        addManualAimButton(ui)
    }

    private fun addManualAimButton(ui: Table) {
        if (autoAim) return

        val cell = hudSystem.addButton(
            ui,
            "icon_manual_aim_sky",
            hudButtons.manualAimButtonClickListener,
        ).size(MANUAL_AIM_BUTTON_SIZE)
        hudButtons.manualAimButton = cell.actor
        cell.right().top()
    }

    companion object {
        private const val MANUAL_AIM_BUTTON_SIZE = 150F
        private const val RADAR_PADDING_HORIZONTAL = 40F
    }
}
