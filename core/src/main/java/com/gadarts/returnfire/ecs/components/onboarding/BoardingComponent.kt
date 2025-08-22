package com.gadarts.returnfire.ecs.components.onboarding

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.ecs.components.character.CharacterColor

class BoardingComponent(val color: CharacterColor, val boardingAnimation: BoardingAnimation?) : Component {
    fun isBoarding(): Boolean {
        return boardingStatus != 0
    }

    fun onBoard() {
        boardingStatus = -1
    }

    fun isDeploying(): Boolean {
        return boardingStatus > 0
    }

    fun isOnboarding(): Boolean {
        return boardingStatus < 0
    }

    fun boardingDone() {
        boardingStatus = 0
    }

    var offBoardSoundId: Long = 0
    private var boardingStatus = 1
    val creationTime = TimeUtils.millis()
}
