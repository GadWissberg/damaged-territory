package com.gadarts.shared.data.type

import com.gadarts.shared.data.definitions.AmbDefinition
import com.gadarts.shared.data.definitions.ElementDefinition

enum class ElementType(val definitions: List<ElementDefinition>) {
    CHARACTER(CharacterType.entries.flatMap { type -> type.values.toList() }),
    AMB(AmbDefinition.entries)
}
