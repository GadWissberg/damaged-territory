package com.gadarts.returnfire.components.onboarding

import com.badlogic.ashley.core.Component

class OnboardingCharacterComponent(val onboardingAnimation: OnboardingAnimation?) : Component {
    fun onBoardingDone() {
        onboarding = false
    }

    var onboarding = true
        private set
}
