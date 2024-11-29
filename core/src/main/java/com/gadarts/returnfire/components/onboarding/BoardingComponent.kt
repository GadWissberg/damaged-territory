package com.gadarts.returnfire.components.onboarding

import com.badlogic.ashley.core.Component

class BoardingComponent(val boardingAnimation: BoardingAnimation?) : Component {
    fun isBoarding(): Boolean {
        return boardingStatus != 0
    }

    fun offBoard() {
        boardingStatus = -1
    }

    fun isOnboarding(): Boolean {
        return boardingStatus > 0
    }

    fun boardingDone() {
        boardingStatus = 0
    }

    private var boardingStatus = 1
}
