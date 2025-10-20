package com.gadarts.returnfire.ecs.systems.ai.logic.path

import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder
import com.gadarts.returnfire.ecs.systems.data.map.GameSessionDataMap
import com.gadarts.returnfire.graph.MapGraphCost
import com.gadarts.returnfire.graph.MapGraphNode
import com.gadarts.returnfire.graph.MapGraphPath
import com.gadarts.returnfire.graph.MapGraphType

class MapPathFinder(private val mapData: GameSessionDataMap, private val pathHeuristic: PathHeuristic) {
    private val pathFinder: IndexedAStarPathFinder<MapGraphNode> by lazy {
        IndexedAStarPathFinder(mapData.mapGraph)
    }

    fun searchNodePath(
        start: MapGraphNode,
        end: MapGraphNode,
        path: MapGraphPath,
        nodesToExclude: MutableList<MapGraphNode>,
        maxCost: MapGraphCost,
    ): Boolean {
        nodesToExclude.forEach { node ->
            node.type = MapGraphType.BLOCKED
        }
        mapData.mapGraph.maxCost = maxCost
        val searchNodePath = pathFinder.searchNodePath(start, end, pathHeuristic, path)
        nodesToExclude.forEach { node ->
            node.type = MapGraphType.AVAILABLE
        }
        return searchNodePath
    }

}
