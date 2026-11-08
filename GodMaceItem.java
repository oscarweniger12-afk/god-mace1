package de.godmace;

import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Fähigkeiten der God Mace:
 *  - Treffer:               Lebensraub (+1 Herz-Hälfte), 20 % Götterblitz (+heilt dich)
 *  - Treffer im freien Fall: Einschlag mit Schockwelle (Schaden + Rückstoß im Umkreis)
 *  - Rechtsklick:           Himmelssprung in Blickrichtung
 *  - Schleichen+Rechtsklick: Göttlicher Zorn - Blitze auf alle Monster im Umkreis von 12 Blöcken
 */
public class GodMaceItem extends Item {
    private static final int LEAP_COOLDOWN = 60;     // 3 Sekunden
    private static final int WRATH_COOLDOWN = 600;   // 30 Sekunden
    private static final int MAX_WRATH_TARGETS = 12;

    public GodMaceItem(Properties properties) {
        super(properties);
    }

    // ---------- Treffer ----------
    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(attacker.level() instanceof ServerLevel level)) {
            return;
        }

        attacker.heal(1.0F); // Lebensraub

        double fall = attacker.fallDistance;
        if (fall > 1.5 && !attacker.onGround()) {
            smash(level, target, attacker, fall);
        }

        if (attacker.getRandom().nextFloat() < 0.20F) {
            strike(level, target, attacker);
        }
    }

    private static void smash(ServerLevel level, LivingEntity target, LivingEntity attacker, double fall) {
        float power = (float) Math.min(fall, 25.0);
        attacker.fallDistance = 0; // kein Fallschaden nach dem Einschlag

        DamageSource source = attacker instanceof Player player
                ? level.damageSources().playerAttack(player)
                : level.damageSources().mobAttack(attacker);

        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class,
                target.getBoundingBox().inflate(4.0),
                entity -> entity != attacker && entity != target && entity.isAlive());

        for (LivingEntity victim : victims) {
            victim.hurtServer(level, source, power * 0.75F);
            launch(victim, attacker, 0.9);
        }
        launch(target, attacker, 1.1);
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, target.getX(), target.getY() + 0.2, target.getZ(),
                1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 0.5, target.getZ(),
                60, 1.5, 0.4, 1.5, 0.4);
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.2F, 1.4F);
    }

    private static void strike(ServerLevel level, LivingEntity target, LivingEntity attacker) {
        // Schutz vor dem eigenen Blitz
        attacker.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 80, 0));
        attacker.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 4));
        EntityType.LIGHTNING_BOLT.spawn(level, target.blockPosition(), EntitySpawnReason.TRIGGERED);
        attacker.heal(4.0F);
    }

    // ---------- Rechtsklick ----------
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel serverLevel) {
            boolean wrath = player.isShiftKeyDown();
            boolean used = wrath ? wrath(serverLevel, player) : leap(serverLevel, player);
            if (!used) {
                return InteractionResult.FAIL;
            }
            player.getCooldowns().addCooldown(player.getItemInHand(hand), wrath ? WRATH_COOLDOWN : LEAP_COOLDOWN);
        }
        return InteractionResult.SUCCESS;
    }

    private static boolean leap(ServerLevel level, Player player) {
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(look.x * 0.9, 0.95, look.z * 0.9);
        player.hurtMarked = true;
        player.fallDistance = 0;
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 1)); // weicher landen

        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 0.1, player.getZ(),
                30, 0.4, 0.1, 0.4, 0.2);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.6F, 1.8F);
        return true;
    }

    private static boolean wrath(ServerLevel level, Player player) {
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(12.0),
                entity -> entity instanceof Enemy && entity.isAlive());

        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("item.godmace.god_mace.no_enemies"), true);
            return false;
        }

        // Schutz vor den eigenen Blitzen
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 4));

        int count = 0;
        for (LivingEntity victim : targets) {
            if (count++ >= MAX_WRATH_TARGETS) {
                break;
            }
            EntityType.LIGHTNING_BOLT.spawn(level, victim.blockPosition(), EntitySpawnReason.TRIGGERED);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0F, 0.8F);
        return true;
    }

    // ---------- Hilfsmethode ----------
    private static void launch(LivingEntity victim, LivingEntity from, double strength) {
        Vec3 away = new Vec3(victim.getX() - from.getX(), 0, victim.getZ() - from.getZ());
        if (away.lengthSqr() < 1.0E-4) {
            away = new Vec3(0, 0, 1);
        }
        away = away.normalize().scale(strength);
        victim.setDeltaMovement(victim.getDeltaMovement().add(away.x, 0.5, away.z));
        victim.hurtMarked = true;
    }
}
