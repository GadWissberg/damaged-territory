package com.gadarts.returnfire.screens.types.hangar.scene.elevator

import com.gadarts.returnfire.managers.SoundManager
import com.gadarts.returnfire.screens.types.gameplay.ToGamePlayScreenSwitchParameters
import com.gadarts.returnfire.screens.types.hangar.HangarScreenMenu
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.assets.definitions.SoundDefinition

class HangarElevatorHandler(
    private val soundManager: SoundManager,
    private val assetsManager: GameAssetManager,
    private val hangarScreenMenu: HangarScreenMenu
) {
    private var elevatorSoundMoveSoundId: Long = -1
    private var deployingState = 0

    fun update(selected: VehicleElevator?, delta: Float): Boolean {
        val reachedDestination = selected!!.updateLocation(delta, deployingState)
        if (reachedDestination) {
            if (elevatorSoundMoveSoundId > -1) {
                soundManager.stop(
                    assetsManager.getAssetByDefinition(SoundDefinition.STAGE_MOVE),
                    elevatorSoundMoveSoundId
                )
                elevatorSoundMoveSoundId = -1
                soundManager.play(
                    assetsManager.getAssetByDefinition(SoundDefinition.STAGE_DEPLOY)
                )
            }
            if (deployingState > 0) {
                hangarScreenMenu.switchToGameplayScreen(
                    ToGamePlayScreenSwitchParameters(
                        selected.characterDefinition,
                        hangarScreenMenu.isAutoAimSelected()
                    )
                )
            } else {
                hangarScreenMenu.show()
                return true
            }
        }
        return false
    }

    fun returnFromCombat() {
        elevatorSoundMoveSoundId = soundManager.play(assetsManager.getAssetByDefinition(SoundDefinition.STAGE_MOVE))
        deployingState = -1
    }

    fun onSelectCharacter() {
        deployingState = 1
    }
}
