package com.gadarts.returnfire.systems.map

import com.gadarts.returnfire.managers.GamePlayManagers

class AmbEffectsHandlers(
    gamePlayManagers: GamePlayManagers,
    mapSystem: MapSystem,
    mapSystemRelatedEntities: MapSystemRelatedEntities
) {
    val fallingBuildingsHandler = FallingBuildingsHandler(mapSystem, gamePlayManagers)
    val waterSplashHandler = WaterSplashHandler(gamePlayManagers.ecs.engine)
    val fadingAwayHandler: FadingAwayHandler = FadingAwayHandler(gamePlayManagers.ecs.engine)
    val ambSoundsHandler = AmbSoundsHandler()
    val groundTextureAnimationHandler = GroundTextureAnimationHandler(gamePlayManagers.ecs.engine)
    val treeEffectsHandler = TreeEffectsHandler(mapSystemRelatedEntities, mapSystem)

}
