package com.gadarts.shared.assets.definitions.external

open class ExternalDefinitions<T>(val definitions: Map<String, T>) {
    companion object {
        const val FORMAT: String = "json"
    }
}
