package com.gadarts.shared.assets.settings

import com.gadarts.shared.data.definitions.characters.CharacterDefinition
import com.gadarts.shared.data.definitions.characters.SimpleCharacterDefinition
import com.gadarts.shared.data.definitions.characters.TurretCharacterDefinition
import com.google.gson.*
import java.lang.reflect.Type

class CharacterDefinitionTypeAdapter : JsonDeserializer<CharacterDefinition?>, JsonSerializer<CharacterDefinition?> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): CharacterDefinition? {
        if (json.isJsonNull) return null

        val characterDefinitions =
            listOf(SimpleCharacterDefinition.entries, TurretCharacterDefinition.entries).flatten()
        for (definition in characterDefinitions) {
            if (definition.name.equals(json.asString, ignoreCase = true)) {
                return definition
            }
        }

        return null
    }

    override fun serialize(src: CharacterDefinition?, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return if (src == null) JsonNull.INSTANCE else JsonPrimitive((src as SimpleCharacterDefinition).name.lowercase())
    }
}

