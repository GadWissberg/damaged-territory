package com.gadarts.shared.assets.settings

import com.gadarts.shared.data.definitions.characters.CharacterDefinition
import com.gadarts.shared.data.definitions.characters.SimpleCharacterDefinition
import com.google.gson.*
import java.lang.reflect.Type

class CharacterDefinitionTypeAdapter : JsonDeserializer<CharacterDefinition?>, JsonSerializer<CharacterDefinition?> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): CharacterDefinition? {
        return if (json.isJsonNull) null else SimpleCharacterDefinition.valueOf(json.asString.uppercase())
    }

    override fun serialize(src: CharacterDefinition?, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return if (src == null) JsonNull.INSTANCE else JsonPrimitive((src as SimpleCharacterDefinition).name.lowercase())
    }
}

