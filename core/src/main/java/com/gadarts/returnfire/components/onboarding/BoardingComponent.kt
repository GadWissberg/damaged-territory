package com.gadarts.returnfire.components.onboarding

import com.badlogic.ashley.core.Component
import com.gadarts.returnfire.components.character.CharacterColor

class BoardingComponent(val color: CharacterColor, val boardingAnimation: BoardingAnimation?) : Component {
    fun isBoarding(): Boolean {
        return boardingStatus != 0
    }

    fun onBoard() {
        boardingStatus = -1
    }

    fun isOffboarding(): Boolean {
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
}
