package com.gadarts.returnfire.systems.ai

import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder
import com.gadarts.returnfire.managers.PathHeuristic
import com.gadarts.returnfire.model.MapGraphPath
import com.gadarts.returnfire.model.MapGraphType
import com.gadarts.returnfire.model.graph.MapGraphNode
import com.gadarts.returnfire.systems.data.GameSessionDataMap

class MapPathFinder(private val mapData: GameSessionDataMap, private val pathHeuristic: PathHeuristic) {
    private val pathFinder: IndexedAStarPathFinder<MapGraphNode> by lazy {
        IndexedAStarPathFinder(mapData.mapGraph)
    }

    fun searchNodePath(
        start: MapGraphNode,
        end: MapGraphNode,
        path: MapGraphPath,
        nodesToExclude: MutableList<MapGraphNode>,
    ): Boolean {
        nodesToExclude.forEach { node ->
            node.type = MapGraphType.BLOCKED
        }
        val searchNodePath = pathFinder.searchNodePath(start, end, pathHeuristic, path)
        nodesToExclude.forEach { node ->
            node.type = MapGraphType.AVAILABLE
        }
        return searchNodePath
    }

}
