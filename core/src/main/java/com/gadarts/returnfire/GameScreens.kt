package com.gadarts.returnfire

import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.gadarts.returnfire.ecs.systems.data.OpponentData
import com.gadarts.returnfire.managers.SoundManager
import com.gadarts.returnfire.screens.ScreensManager
import com.gadarts.returnfire.screens.types.gameplay.GamePlayScreen
import com.gadarts.returnfire.screens.types.hangar.HangarScreenImpl
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.data.CharacterColor

class GameScreens(
    dispatcher: MessageDispatcher,
    runsOnMobile: Boolean,
    assetsManager: GameAssetManager,
    screensManager: ScreensManager,
    soundManager: SoundManager,
    opponentsData: Map<CharacterColor, OpponentData>
) {
    var gamePlayScreen: GamePlayScreen? = null
    val hangarScreen by lazy {
        HangarScreenImpl(
            dispatcher,
            runsOnMobile,
            assetsManager,
            screensManager,
            soundManager,
            opponentsData
        )
    }

}
