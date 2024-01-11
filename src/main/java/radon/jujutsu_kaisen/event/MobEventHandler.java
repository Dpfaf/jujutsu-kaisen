package radon.jujutsu_kaisen.event;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.SleepFinishedTimeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import radon.jujutsu_kaisen.ChantHandler;
import radon.jujutsu_kaisen.JujutsuKaisen;
import radon.jujutsu_kaisen.VeilHandler;
import radon.jujutsu_kaisen.ability.*;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.ability.misc.Barrage;
import radon.jujutsu_kaisen.ability.misc.Slam;
import radon.jujutsu_kaisen.capability.data.ISorcererData;
import radon.jujutsu_kaisen.capability.data.SorcererDataHandler;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;
import radon.jujutsu_kaisen.capability.data.sorcerer.JujutsuType;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererGrade;
import radon.jujutsu_kaisen.capability.data.sorcerer.Trait;
import radon.jujutsu_kaisen.config.ConfigHolder;
import radon.jujutsu_kaisen.damage.JJKDamageSources;
import radon.jujutsu_kaisen.effect.JJKEffects;
import radon.jujutsu_kaisen.entity.base.ISorcerer;
import radon.jujutsu_kaisen.entity.base.JJKPartEntity;
import radon.jujutsu_kaisen.entity.projectile.ThrownChainProjectile;
import radon.jujutsu_kaisen.entity.sorcerer.HeianSukunaEntity;
import radon.jujutsu_kaisen.entity.sorcerer.SukunaEntity;
import radon.jujutsu_kaisen.item.CursedEnergyFleshItem;
import radon.jujutsu_kaisen.item.JJKItems;
import radon.jujutsu_kaisen.item.base.CursedToolItem;
import radon.jujutsu_kaisen.network.PacketHandler;
import radon.jujutsu_kaisen.network.packet.s2c.SyncSorcererDataS2CPacket;
import radon.jujutsu_kaisen.util.CuriosUtil;
import radon.jujutsu_kaisen.util.HelperMethods;
import radon.jujutsu_kaisen.util.PlayerUtil;
import radon.jujutsu_kaisen.util.SorcererUtil;

import java.util.ArrayList;
import java.util.List;

public class MobEventHandler {
    @Mod.EventBusSubscriber(modid = JujutsuKaisen.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class JJKEventHandlerForgeEvents {
        @SubscribeEvent
        public static void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
            if (!VeilHandler.canSpawn(event.getEntity(), event.getX(), event.getY(), event.getZ())) {
                event.setSpawnCancelled(true);
            }
        }

        @SubscribeEvent
        public static void onLivingAttack(LivingAttackEvent event) {
            DamageSource source = event.getSource();

            if (!(source.getEntity() instanceof LivingEntity attacker)) return;

            LivingEntity victim = event.getEntity();

            if (victim.level().isClientSide) return;

            // Checks to prevent tamed creatures from attacking their owners and owners from attacking their tames
            if (attacker instanceof TamableAnimal tamable1 && attacker instanceof ISorcerer) {
                if (tamable1.isTame() && tamable1.getOwner() == victim) {
                    event.setCanceled(true);
                } else if (victim instanceof TamableAnimal tamable2 && victim instanceof ISorcerer) {
                    // Prevent tames with the same owner from attacking each other
                    if (!tamable1.is(tamable2) && tamable1.isTame() && tamable2.isTame() && tamable1.getOwner() == tamable2.getOwner()) {
                        event.setCanceled(true);
                    }
                }
            } else if (victim instanceof TamableAnimal tamable && victim instanceof ISorcerer) {
                // Prevent the owner from attacking the tame
                if (tamable.isTame() && tamable.getOwner() == attacker) {
                    event.setCanceled(true);
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onLivingHurt(LivingHurtEvent event) {
            LivingEntity victim = event.getEntity();

            if (victim.level().isClientSide) return;

            DamageSource source = event.getSource();

            if (victim instanceof ISorcerer sorcerer && sorcerer.canPerformSorcery()) {
                if (!source.is(DamageTypeTags.BYPASSES_ARMOR)) {
                    if (!JJKAbilities.hasToggled(victim, JJKAbilities.CURSED_ENERGY_FLOW.get())) {
                        AbilityHandler.trigger(victim, JJKAbilities.CURSED_ENERGY_FLOW.get());
                    }

                    if (!JJKAbilities.isChanneling(victim, JJKAbilities.CURSED_ENERGY_SHIELD.get())) {
                        AbilityHandler.trigger(victim, JJKAbilities.CURSED_ENERGY_SHIELD.get());
                    }

                    if (source instanceof JJKDamageSources.JujutsuDamageSource) {
                        if (!JJKAbilities.hasToggled(victim, JJKAbilities.DOMAIN_AMPLIFICATION.get())) {
                            AbilityHandler.trigger(victim, JJKAbilities.DOMAIN_AMPLIFICATION.get());
                        }
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onAbilityTrigger(AbilityTriggerEvent.Pre event) {
            Ability ability = event.getAbility();

            LivingEntity owner = event.getEntity();

            ISorcererData cap = owner.getCapability(SorcererDataHandler.INSTANCE).resolve().orElseThrow();

            // Sukuna has multiple arms
            if (owner instanceof HeianSukunaEntity entity && ability == JJKAbilities.BARRAGE.get()) {
                entity.setBarrage(Barrage.DURATION * 2);
            }

            // Making mobs use chants
            if (owner.level() instanceof ServerLevel level) {
                if (owner instanceof Mob) {
                    List<String> chants = new ArrayList<>(cap.getFirstChants(ability));

                    if (!chants.isEmpty() && HelperMethods.RANDOM.nextInt(Math.max(1, (int) (50 * (cap.getEnergy() / cap.getMaxEnergy()) * cap.getMaximumOutput()))) == 0) {
                        for (int i = 0; i < HelperMethods.RANDOM.nextInt(chants.size()); i++) {
                            ChantHandler.onChant(owner, chants.get(i));

                            for (ServerPlayer player : level.players()) {
                                if (player.distanceTo(owner) > 32.0D) continue;

                                ResourceLocation key = owner.level().registryAccess().registryOrThrow(Registries.ENTITY_TYPE).getKey(owner.getType());

                                if (key != null) {
                                    player.sendSystemMessage(Component.literal(String.format("<%s> %s", owner.getName().getString(), chants.get(i))));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}