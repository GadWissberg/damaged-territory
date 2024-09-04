package com.gadarts.returnfire.model

enum class ElementType(val definitions: List<ElementDefinition>) {
    CHARACTER(CharacterType.entries.flatMap { type -> type.values.toList() }),
    AMB(AmbDefinition.entries)
}
