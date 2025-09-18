package com.gadarts.returnfire.ecs.systems.camera

import com.gadarts.shared.data.definitions.characters.SimpleCharacterDefinition
import com.gadarts.shared.data.definitions.characters.TurretCharacterDefinition

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
        ),
        TurretCharacterDefinition.JEEP to CameraRelativeValues(
            6F, 5F,
            8F, 5F,
            6F, 6F
        ),
    )
}
