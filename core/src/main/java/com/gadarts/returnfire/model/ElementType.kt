package com.gadarts.returnfire.model

import com.gadarts.returnfire.model.definitions.AmbDefinition
import com.gadarts.returnfire.model.definitions.ElementDefinition

enum class ElementType(val definitions: List<ElementDefinition>) {
    CHARACTER(CharacterType.entries.flatMap { type -> type.values.toList() }),
    AMB(AmbDefinition.entries)
}
