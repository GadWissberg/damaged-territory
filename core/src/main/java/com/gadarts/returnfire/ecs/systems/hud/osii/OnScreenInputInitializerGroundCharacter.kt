package com.gadarts.returnfire.ecs.systems.hud.osii

import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad
import com.gadarts.returnfire.ecs.systems.hud.HudButtons
import com.gadarts.returnfire.ecs.systems.hud.HudSystem
import com.gadarts.returnfire.ecs.systems.hud.HudSystem.Companion.HUD_ELEMENT_PADDING_BOTTOM
import com.gadarts.returnfire.ecs.systems.hud.HudSystem.Companion.JOYSTICK_PADDING_HORIZONTAL
import com.gadarts.shared.assets.settings.GameSettings

class OnScreenInputInitializerGroundCharacter(
    private val hudSystem: HudSystem,
    private val hudButtons: HudButtons,
    private val turretTouchpad: Touchpad?,
    private val hideAttackButtons: Boolean,
    private val gameSettings: GameSettings,
) :
        (Table, Cell<Touchpad>) -> Unit {
    override fun invoke(ui: Table, movementPad: Cell<Touchpad>) {
        val reverseButton = hudSystem.addButton(
            ui,
            "icon_reverse",
            hudButtons.reverseButtonClickListener
        )
        reverseButton.grow().left().bottom().padBottom(32F)
        hudSystem.addRadar(ui)
            .pad(0F, RADAR_PADDING_HORIZONTAL, HUD_ELEMENT_PADDING_BOTTOM, RADAR_PADDING_HORIZONTAL)
            .bottom()
        if (!hideAttackButtons) {
            val attackButtonsTable = Table()
            attackButtonsTable.setDebug(gameSettings.uiDebug, true)
            hudSystem.addButton(
                attackButtonsTable,
                "icon_missiles",
                hudButtons.secWeaponButtonClickListener,
            ).center().row()
            ui.add(attackButtonsTable).right().bottom()
                .pad(0F, 0F, HUD_ELEMENT_PADDING_BOTTOM, 0F).bottom()
        }
        if (turretTouchpad != null) {
            hudSystem.addTouchpad(ui, turretTouchpad)
                .pad(0F, 0F, HUD_ELEMENT_PADDING_BOTTOM, JOYSTICK_PADDING_HORIZONTAL).bottom()
        }
    }

    companion object {
        private const val RADAR_PADDING_HORIZONTAL = 40F
    }
}
