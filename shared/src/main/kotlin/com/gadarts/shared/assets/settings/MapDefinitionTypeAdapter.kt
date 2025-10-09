package com.gadarts.shared.assets.settings

import com.gadarts.shared.assets.definitions.MapDefinition
import com.google.gson.*
import java.lang.reflect.Type

class MapDefinitionTypeAdapter : JsonDeserializer<MapDefinition>, JsonSerializer<MapDefinition> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): MapDefinition {
        return MapDefinition.valueOf(json.asString.uppercase())
    }

    override fun serialize(src: MapDefinition, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.name.lowercase())
    }
}

