package com.gadarts.returnfire.systems.camera

import com.gadarts.returnfire.model.definitions.SimpleCharacterDefinition
import com.gadarts.returnfire.model.definitions.TurretCharacterDefinition

class CameraRelativeValuesMapper {
    val mapping = mapOf(
        TurretCharacterDefinition.TANK to CameraRelativeValues(
            4F, 3F,
            10F, 4F,
            7F, 4F
        ),
        SimpleCharacterDefinition.APACHE to CameraRelativeValues(
            4F, 3F,
            8F, 3F,
            6F, 4F
        )
    )
}