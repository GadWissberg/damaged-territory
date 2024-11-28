package com.gadarts.returnfire.components

import com.badlogic.ashley.core.ComponentMapper
import com.gadarts.returnfire.components.arm.PrimaryArmComponent
import com.gadarts.returnfire.components.arm.SecondaryArmComponent
import com.gadarts.returnfire.components.bullet.BulletComponent
import com.gadarts.returnfire.components.cd.ChildDecalComponent
import com.gadarts.returnfire.components.model.ModelInstanceComponent
import com.gadarts.returnfire.components.onboarding.OnboardingCharacterComponent
import com.gadarts.returnfire.components.physics.PhysicsComponent

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
    val enemy: ComponentMapper<EnemyComponent> =
        ComponentMapper.getFor(EnemyComponent::class.java)
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
    val onboardingCharacter: ComponentMapper<OnboardingCharacterComponent> =
        ComponentMapper.getFor(OnboardingCharacterComponent::class.java)
    val baseDoor: ComponentMapper<BaseDoorComponent> =
        ComponentMapper.getFor(BaseDoorComponent::class.java)
    val childModelInstanceComponent: ComponentMapper<ChildModelInstanceComponent> =
        ComponentMapper.getFor(ChildModelInstanceComponent::class.java)

}
