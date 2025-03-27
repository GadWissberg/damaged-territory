package com.gadarts.returnfire.model.graph

data class GraphNode(val index: Int, val x: Int, val y: Int) {
    override fun toString(): String = "Node($index, x=$x, y=$y)"
}