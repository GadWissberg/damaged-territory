package com.gadarts.returnfire.assets.definitions.external

open class ExternalDefinitions<T>(val definitions: Map<String, T>) {
    companion object {
        const val FORMAT: String = "json"
    }
}
