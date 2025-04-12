package com.gadarts.returnfire.components

import com.badlogic.ashley.core.ComponentMapper
import com.gadarts.returnfire.components.ai.AiComponent
import com.gadarts.returnfire.components.ai.AiTurretComponent
import com.gadarts.returnfire.components.amb.AmbAnimationComponent
import com.gadarts.returnfire.components.amb.AmbComponent
import com.gadarts.returnfire.components.amb.AmbCorpsePart
import com.gadarts.returnfire.components.amb.AmbSoundComponent
import com.gadarts.returnfire.components.arm.PrimaryArmComponent
import com.gadarts.returnfire.components.arm.SecondaryArmComponent
import com.gadarts.returnfire.components.bullet.BulletComponent
import com.gadarts.returnfire.components.cd.ChildDecalComponent
import com.gadarts.returnfire.components.effects.CrashingAircraftEmitter
import com.gadarts.returnfire.components.effects.GroundBlastComponent
import com.gadarts.returnfire.components.effects.ParticleEffectComponent
import com.gadarts.returnfire.components.effects.SparkComponent
import com.gadarts.returnfire.components.model.ModelInstanceComponent
import com.gadarts.returnfire.components.onboarding.BoardingComponent
import com.gadarts.returnfire.components.physics.GhostPhysicsComponent
import com.gadarts.returnfire.components.physics.PhysicsComponent
import com.gadarts.returnfire.components.pit.BaseComponent
import com.gadarts.returnfire.components.pit.BaseDoorComponent
import com.gadarts.returnfire.components.turret.TurretBaseComponent
import com.gadarts.returnfire.components.turret.TurretComponent

object ComponentsMapper {
    val modelInstance: ComponentMapper<ModelInstanceComponent> =
        ComponentMapper.getFor(ModelInstanceComponent::class.java)
    val childDecal: ComponentMapper<ChildDecalComponent> =
        ComponentMapper.getFor(ChildDecalComponent::class.java)
    val ambSound: ComponentMapper<AmbSoundComponent> =
        ComponentMapper.getFor(AmbSoundComponent::class.java)
    val player: ComponentMapper<PlayerComponent> =
        ComponentMapper.getFor(PlayerComponent::class.java)
    val primaryArm: ComponentMapper<PrimaryArmComponent> =
        ComponentMapper.getFor(PrimaryArmComponent::class.java)
    val secondaryArm: ComponentMapper<SecondaryArmComponent> =
        ComponentMapper.getFor(SecondaryArmComponent::class.java)
    val bullet: ComponentMapper<BulletComponent> =
        ComponentMapper.getFor(BulletComponent::class.java)
    val amb: ComponentMapper<AmbComponent> =
        ComponentMapper.getFor(AmbComponent::class.java)
    val independentDecal: ComponentMapper<IndependentDecalComponent> =
        ComponentMapper.getFor(IndependentDecalComponent::class.java)
    val particleEffect: ComponentMapper<ParticleEffectComponent> =
        ComponentMapper.getFor(ParticleEffectComponent::class.java)
    val animatedTexture: ComponentMapper<AnimatedTextureComponent> =
        ComponentMapper.getFor(AnimatedTextureComponent::class.java)
    val physics: ComponentMapper<PhysicsComponent> =
        ComponentMapper.getFor(PhysicsComponent::class.java)
    val ai: ComponentMapper<AiComponent> =
        ComponentMapper.getFor(AiComponent::class.java)
    val aiTurret: ComponentMapper<AiTurretComponent> =
        ComponentMapper.getFor(AiTurretComponent::class.java)
    val character: ComponentMapper<CharacterComponent> =
        ComponentMapper.getFor(CharacterComponent::class.java)
    val spark: ComponentMapper<SparkComponent> =
        ComponentMapper.getFor(SparkComponent::class.java)
    val ground: ComponentMapper<GroundComponent> =
        ComponentMapper.getFor(GroundComponent::class.java)
    val waterWave: ComponentMapper<GroundBlastComponent> =
        ComponentMapper.getFor(GroundBlastComponent::class.java)
    val turret: ComponentMapper<TurretComponent> =
        ComponentMapper.getFor(TurretComponent::class.java)
    val turretBase: ComponentMapper<TurretBaseComponent> =
        ComponentMapper.getFor(TurretBaseComponent::class.java)
    val boarding: ComponentMapper<BoardingComponent> =
        ComponentMapper.getFor(BoardingComponent::class.java)
    val baseDoor: ComponentMapper<BaseDoorComponent> =
        ComponentMapper.getFor(BaseDoorComponent::class.java)
    val hangar: ComponentMapper<BaseComponent> =
        ComponentMapper.getFor(BaseComponent::class.java)
    val stage: ComponentMapper<StageComponent> =
        ComponentMapper.getFor(StageComponent::class.java)
    val childModelInstance: ComponentMapper<ChildModelInstanceComponent> =
        ComponentMapper.getFor(ChildModelInstanceComponent::class.java)
    val autoAim: ComponentMapper<AutoAimComponent> =
        ComponentMapper.getFor(AutoAimComponent::class.java)
    val crashingAircraftEmitter: ComponentMapper<CrashingAircraftEmitter> =
        ComponentMapper.getFor(CrashingAircraftEmitter::class.java)
    val limitedVelocity: ComponentMapper<LimitedVelocityComponent> =
        ComponentMapper.getFor(LimitedVelocityComponent::class.java)
    val road: ComponentMapper<RoadComponent> =
        ComponentMapper.getFor(RoadComponent::class.java)
    val ambAnimation: ComponentMapper<AmbAnimationComponent> =
        ComponentMapper.getFor(AmbAnimationComponent::class.java)
    val ghostPhysics: ComponentMapper<GhostPhysicsComponent> =
        ComponentMapper.getFor(GhostPhysicsComponent::class.java)
    val deathSequence: ComponentMapper<DeathSequenceComponent> =
        ComponentMapper.getFor(DeathSequenceComponent::class.java)
    val ambCorpsePart: ComponentMapper<AmbCorpsePart> =
        ComponentMapper.getFor(AmbCorpsePart::class.java)
    val fence: ComponentMapper<FenceComponent> =
        ComponentMapper.getFor(FenceComponent::class.java)
}
