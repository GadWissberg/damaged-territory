package com.gadarts.returnfire.ecs.systems.ai.logic

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.physics.bullet.collision.btPairCachingGhostObject
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.components.ai.BaseAiComponent
import com.gadarts.returnfire.ecs.components.turret.TurretComponent
import com.gadarts.returnfire.ecs.systems.ai.logic.goals.AiOpponentGoal
import com.gadarts.returnfire.ecs.systems.ai.logic.ground.AiJeepCharacterLogic
import com.gadarts.returnfire.ecs.systems.ai.logic.ground.AiTankCharacterLogic
import com.gadarts.returnfire.ecs.systems.data.session.GameSessionData
import com.gadarts.returnfire.ecs.systems.events.SystemEvents
import com.gadarts.returnfire.ecs.systems.events.data.OpponentEnteredGameplayScreenEventData
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.shared.data.CharacterColor
import com.gadarts.shared.data.definitions.characters.CharacterDefinition
import com.gadarts.shared.data.definitions.characters.SimpleCharacterDefinition
import com.gadarts.shared.data.definitions.characters.TurretCharacterDefinition
import java.util.*

class AiLogicHandler(
    private val gameSessionData: GameSessionData,
    private val aiCharacterEntities: ImmutableArray<Entity>,
    private val gamePlayManagers: GamePlayManagers,
    autoAim: btPairCachingGhostObject,
    engine: Engine
) : Disposable {

    private val randomizedFightersList = mutableListOf<CharacterDefinition>()
    private var lastDeployTime: Long = 0
    private var lastDeployedCharacter: CharacterDefinition = SimpleCharacterDefinition.APACHE
    private val enemyTurretEntities: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(Family.all(TurretComponent::class.java, BaseAiComponent::class.java).get())
    }
    private val aiEnemyTurretLogic by lazy { AiEnemyTurretLogic(gameSessionData, gamePlayManagers) }

    private val logics = mapOf(
        SimpleCharacterDefinition.APACHE to AiApacheLogic(
            gameSessionData,
            gamePlayManagers,
            autoAim
        ),
        TurretCharacterDefinition.TANK to AiTankCharacterLogic(
            gameSessionData,
            autoAim,
            gamePlayManagers
        ),
        TurretCharacterDefinition.JEEP to AiJeepCharacterLogic(
            gameSessionData,
            gamePlayManagers,
            autoAim,
        )
    )

    fun update(deltaTime: Float) {
        updateEnemyTurrets(deltaTime)
        if (!gamePlayManagers.assetsManager.gameSettings.aiVehicleDisabled) {
            for (i in 0 until aiCharacterEntities.size()) {
                val character = aiCharacterEntities[i]
                val boardingComponent = ComponentsMapper.boarding.get(character)
                val characterComponent = ComponentsMapper.character.get(
                    character
                )

                if ((boardingComponent != null && boardingComponent.isBoarding())
                    || characterComponent == null
                    || characterComponent.dead
                ) continue

                val logic = logics[characterComponent.definition]
                if (logic != null) {
                    updateLogic(character, deltaTime, logic)
                }
            }
        }
        val currentIsFighter = lastDeployedCharacter != TurretCharacterDefinition.JEEP
        if (currentIsFighter && gameSessionData.aiOpponentGoal != AiOpponentGoal.BRING_BACK_CHARACTER && TimeUtils.timeSinceMillis(
                lastDeployTime
            ) > MAX_TIME_TO_CHANGE_CHARACTER_IN_MINUTES * 60 * 1000
        ) {
            gameSessionData.aiOpponentGoal = AiOpponentGoal.BRING_BACK_CHARACTER
        }
    }

    private fun updateEnemyTurrets(deltaTime: Float) {
        for (turret in enemyTurretEntities) {
            val characterComponent = ComponentsMapper.character.get(ComponentsMapper.turret.get(turret).base)
            if (characterComponent == null || characterComponent.dead || ComponentsMapper.deathSequence.has(turret)) continue

            aiEnemyTurretLogic.attack(deltaTime, turret)
        }
    }

    private fun updateLogic(character: Entity, deltaTime: Float, logic: AiCharacterLogic) {
        logic.preUpdate(character, deltaTime)
        logic.update(character, deltaTime)
    }

    override fun dispose() {
        logics.forEach { (_, aiLogic) ->
            aiLogic.dispose()
        }
    }

    fun begin() {
        gameSessionData.gamePlayData.opponentsData[CharacterColor.GREEN]?.vehicleAmounts?.forEach { (characterDefinition, amount) ->
            if (characterDefinition != TurretCharacterDefinition.JEEP) {
                for (i in 0 until amount) {
                    randomizedFightersList.add(characterDefinition)
                }
            }
        }
        randomizedFightersList.shuffle(Random(System.currentTimeMillis()))
        deployNextCharacter()
    }

    private fun deployCharacter(selectedCharacter: CharacterDefinition) {
        OpponentEnteredGameplayScreenEventData.set(
            CharacterColor.GREEN,
            selectedCharacter
        )
        gamePlayManagers.dispatcher.dispatchMessage(
            SystemEvents.OPPONENT_ENTERED_GAME_PLAY_SCREEN.ordinal,
        )
        lastDeployedCharacter = selectedCharacter
        lastDeployTime = TimeUtils.millis()
        gameSessionData.aiOpponentGoal = null
    }

    fun onVehicleOnboarded(vehicle: Entity) {
        randomizedFightersList.add(ComponentsMapper.character.get(vehicle).definition)
        deployNextCharacter()
    }

    private fun deployNextCharacter() {
        val hasJeeps = (gameSessionData.gamePlayData.opponentsData[CharacterColor.GREEN]?.vehicleAmounts?.get(
            TurretCharacterDefinition.JEEP
        ) ?: 0
            ) > 0
        if (randomizedFightersList.isNotEmpty()) {
            deployCharacter(if (lastDeployedCharacter != TurretCharacterDefinition.JEEP && hasJeeps) TurretCharacterDefinition.JEEP else randomizedFightersList.removeFirst())
        } else if (hasJeeps) {
            deployCharacter(TurretCharacterDefinition.JEEP)
        }
    }

    fun onElevatorEmptyOnboard() {
        deployNextCharacter()
    }

    fun onCharacterDied() {
        gamePlayManagers.dispatcher.dispatchMessage(
            SystemEvents.ELEVATOR_EMPTY_ONBOARD_REQUESTED.ordinal,
            CharacterColor.GREEN
        )
    }

    companion object {
        private const val MAX_TIME_TO_CHANGE_CHARACTER_IN_MINUTES = 5
    }
}
