package com.gadarts.returnfire.ecs.systems.data

import com.gadarts.shared.data.definitions.characters.CharacterDefinition
import com.gadarts.shared.data.type.CharacterType

class OpponentData {
    val vehicleAmounts: MutableMap<CharacterDefinition, Int> =
        CharacterType.entries.flatMap { it.values.toList() }.filter { it.isDeployable() }.associateBy({ it }, { 1 })
            .toMutableMap()
}
