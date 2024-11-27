package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Component

class OnboardingCharacterComponent : Component {
    fun onBoardingDone() {
        onboarding = false
    }

    var onboarding = true
        private set
}
