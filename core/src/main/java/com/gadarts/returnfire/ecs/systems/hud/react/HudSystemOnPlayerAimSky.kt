package com.gadarts.returnfire.ecs.systems.hud.react

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.ecs.systems.HandlerOnEvent
import com.gadarts.returnfire.ecs.systems.data.GameSessionData
import com.gadarts.returnfire.ecs.systems.hud.HudButtons

class HudSystemOnPlayerAimSky(private val hudButtons: HudButtons) : HandlerOnEvent {
    override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        val name =
            if (msg.extraInfo as Boolean) "icon_manual_aim_sky" else "icon_manual_aim_ground"
        if (!gameSessionData.runsOnMobile) {
            val texture =
                gamePlayManagers.assetsManager.getTexture(name)
            val icon = Image(texture)
            icon.setPosition(
                Gdx.graphics.width / 2F - texture.width / 2F,
                Gdx.graphics.height / 2F - texture.height / 2F
            )
            gameSessionData.hudData.stage.addActor(icon)
            icon.addAction(
                Actions.sequence(
                    Actions.fadeOut(1F, Interpolation.fade),
                    Actions.run { icon.remove() })
            )
        } else {
            hudButtons.manualAimButton!!.image.drawable =
                TextureRegionDrawable(gamePlayManagers.assetsManager.getTexture(name))
        }

    }

}
