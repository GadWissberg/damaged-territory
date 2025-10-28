package com.gadarts.returnfire.managers

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.ashley.utils.ImmutableArray
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.components.FlagFloorComponent
import com.gadarts.returnfire.ecs.systems.data.session.GameSessionData
import com.gadarts.returnfire.ecs.systems.events.SystemEvents
import com.gadarts.shared.assets.definitions.SoundDefinition
import com.gadarts.shared.data.CharacterColor

class CtfManager(
    private val generalManagers: GeneralManagers,
    private val gameSessionData: GameSessionData,
    private val engine: PooledEngine
) {
    private val flagFloors: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(
            Family.all(
                FlagFloorComponent::class.java
            ).get()
        )
    }

    fun handleJeepFlagCollision(jeep: Entity, flag: Entity) {
        val characterComponent = ComponentsMapper.character.get(jeep)
        val flagComponent = ComponentsMapper.flag.get(flag)
        val characterColor = characterComponent.color
        val soundManager = generalManagers.soundManager
        val dispatcher = generalManagers.dispatcher
        if (characterColor != flagComponent.color) {
            if (flagComponent.follow == null || ComponentsMapper.flagFloor.has(
                    flagComponent.follow
                )
            ) {
                flagComponent.follow = jeep
                soundManager.play(SoundDefinition.FLAG_TAKEN)
                dispatcher.dispatchMessage(SystemEvents.FLAG_TAKEN.ordinal, flag)
            }
        } else {
            if (flagComponent.follow == null) {
                returnFlag(flag)
            } else {
                val rivalColor =
                    if (characterColor == CharacterColor.GREEN) CharacterColor.BROWN else CharacterColor.GREEN
                if (ComponentsMapper.flag.get(gameSessionData.gamePlayData.flags[rivalColor]).follow == jeep) {
                    dispatcher.dispatchMessage(SystemEvents.FLAG_POINT.ordinal, characterColor)
                }
            }
        }
    }

    fun returnFlag(flag: Entity) {
        val flagComponent = ComponentsMapper.flag.get(flag)
        val flagFloor = flagFloors.first { ComponentsMapper.flagFloor.get(it).color == flagComponent.color }
        flagComponent.follow = flagFloor
        generalManagers.soundManager.play(SoundDefinition.FLAG_RETURNED)
        generalManagers.dispatcher.dispatchMessage(SystemEvents.FLAG_RETURNED.ordinal, flag)
    }

}
