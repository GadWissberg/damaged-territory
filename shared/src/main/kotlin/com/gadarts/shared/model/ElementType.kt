package com.gadarts.shared.model

import com.gadarts.shared.model.definitions.AmbDefinition
import com.gadarts.shared.model.definitions.ElementDefinition

enum class ElementType(val definitions: List<ElementDefinition>) {
    CHARACTER(CharacterType.entries.flatMap { type -> type.values.toList() }),
    AMB(AmbDefinition.entries)
}
