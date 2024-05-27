package com.gadarts.returnfire.systems.events.data

object EntityEnteredNewRegionEventData {
    var prevRow: Int = -1
        private set
    var prevColumn: Int = -1
        private set
    var newRow: Int = -1
        private set
    var newColumn: Int = -1
        private set

    fun set(prevRow: Int, prevColumn: Int, newRow: Int, newColumn: Int) {
        this.prevRow = prevRow
        this.prevColumn = prevColumn
        this.newRow = newRow
        this.newColumn = newColumn
    }

    override fun toString(): String {
        return "PlayerEnteredNewRegionEventData(prevRow=$prevRow, prevColumn=$prevColumn, newRow=$newRow, newColumn=$newColumn)"
    }


}
