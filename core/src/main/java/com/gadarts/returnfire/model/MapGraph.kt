package com.gadarts.returnfire.model

import com.badlogic.gdx.ai.pfa.Connection
import com.badlogic.gdx.ai.pfa.DefaultConnection
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph
import com.badlogic.gdx.utils.Array
import com.gadarts.returnfire.model.graph.MapGraphNode

class MapGraph(val width: Int, val depth: Int) : IndexedGraph<MapGraphNode> {
    private val nodes: kotlin.Array<kotlin.Array<MapGraphNode>> =
        Array(depth) { Array(width) { MapGraphNode(-1, -1, -1, MapGraphType.AVAILABLE) } }
    private val connections = mutableMapOf<MapGraphNode, Array<Connection<MapGraphNode>>>()

    fun addNode(x: Int, y: Int, type: MapGraphType): MapGraphNode {
        val index = y * width + x
        val node = MapGraphNode(index, x, y, type)
        nodes[y][x] = node
        return node
    }

    fun connect(from: MapGraphNode, to: MapGraphNode) {
        connections.computeIfAbsent(from) { Array() }.add(DefaultConnection(from, to))
    }

    override fun getIndex(node: MapGraphNode): Int = node.index

    override fun getConnections(fromNode: MapGraphNode): Array<Connection<MapGraphNode>> {
        auxConnectionsList.clear()
        if (fromNode.type == MapGraphType.BLOCKED) {
            return auxConnectionsList
        }

        connections[fromNode]?.forEach {
            if (it.toNode.type != MapGraphType.BLOCKED) {
                auxConnectionsList.add(it)
            }
        }
        return auxConnectionsList
    }

    override fun getNodeCount(): Int = width * depth

    fun getNode(x: Int, y: Int): MapGraphNode {
        return nodes[y][x]
    }

    companion object {
        private val auxConnectionsList = Array<Connection<MapGraphNode>>(8)
    }
}
