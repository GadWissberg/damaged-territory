package com.gadarts.returnfire.model

class GameMap(val tilesMapping: Array<CharArray>, val placedElements: List<PlacedElement>) {
    companion object {
        const val TILE_TYPE_EMPTY = '0'
    }
}
