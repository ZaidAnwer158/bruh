package com.miraculous;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.text.Text;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class MiraculousMod implements ModInitializer {
    private static final int DURATION_TICKS = 20 * 300;
    private static final Map<UUID, ActiveHero> ACTIVE = new HashMap<>();

    private enum Hero {
        LADYBUG("ladybug", "Ladybug", "Tikki", "Lucky Charm"),
        CATNOIR("catnoir", "Cat Noir", "Plagg", "Cataclysm"),
        RENAROUGE("renarouge", "Rena Rouge", "Trixx", "Mirage"),
        CARAPACE("carapace", "Carapace", "Wayzz", "Shell-ter"),
        QUEENBEE("queenbee", "Queen Bee", "Pollen", "Venom"),
        VIPERION("viperion", "Viperion", "Sass", "Second Chance"),
        PIGELLA("pigella", "Pigella", "Daizzi", "Piggy Power"),
        KINGMONKEY("kingmonkey", "King Monkey", "Xuppu", "Uproar"),
        RYUKO("ryuko", "Ryuko", "Longg", "Dragon Wind"),
        BUNNYX("bunnyx", "Bunnyx", "Fluff", "Burrow"),
        PEGASUS("pegasus", "Pegasus", "Kaalki", "Voyage");

        final String id, display, kwami, power;
        Hero(String id, String display, String kwami, String power) {
            this.id = id; this.display = display; this.kwami = kwami; this.power = power;
        }
        static Hero find(String input) {
            String normalized = input.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
            for (Hero h : values()) if (h.id.equals(normalized) || h.display.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "").equals(normalized)) return h;
            return null;
        }
    }

    private static final class ActiveHero {
        final Hero hero;
        int ticksLeft = DURATION_TICKS;
        ActiveHero(Hero hero) { this.hero = hero; }
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
        ServerTickEvents.END_SERVER_TICK.register(MiraculousMod::tick);
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> d) {
        d.register(CommandManager.literal("miraculous")
            .executes(ctx -> {
                ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                p.sendMessage(Text.literal("§6Miraculous heroes: §e/miraculous list"));
                p.sendMessage(Text.literal("§7Transform: §f/miraculous <hero>§7, power: §f/miraculous power§7, off: §f/miraculous off"));
                return 1;
            })
            .then(CommandManager.literal("list").executes(ctx -> {
                ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                p.sendMessage(Text.literal("§6§l✦ Miraculous heroes ✦"));
                for (Hero h : Hero.values()) p.sendMessage(Text.literal("§e" + h.display + " §7(" + h.kwami + ") §8- §f" + h.power));
                return 1;
            }))
            .then(CommandManager.literal("off").executes(ctx -> {
                ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                detransform(p, "§7Spots off!");
                return 1;
            }))
            .then(CommandManager.literal("power").executes(ctx -> {
                ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                usePower(p);
                return 1;
            }))
            .then(CommandManager.argument("hero", StringArgumentType.word()).suggests((ctx, builder) -> {
                for (Hero h : Hero.values()) builder.suggest(h.id);
                builder.suggest("off").suggest("list").suggest("power");
                return builder.buildFuture();
            }).executes(ctx -> {
                ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                String name = StringArgumentType.getString(ctx, "hero");
                Hero h = Hero.find(name);
                if (h == null) { p.sendMessage(Text.literal("§cUnknown hero. Try /miraculous list.")); return 0; }
                transform(p, h);
                return 1;
            }))
        );
    }

    private static void transform(ServerPlayerEntity p, Hero h) {
        if (ACTIVE.containsKey(p.getUuid())) {
            p.sendMessage(Text.literal("§cYou're already transformed! Use /miraculous off first."));
            return;
        }
        ACTIVE.put(p.getUuid(), new ActiveHero(h));
        applyPassives(p, h, DURATION_TICKS);
        p.sendMessage(Text.literal("§e✦ " + h.kwami + ", transform me! §fYou are now " + h.display + "!"));
        p.sendMessage(Text.literal("§7Power: §f/miraculous power §7| Lasts 5 minutes | §f/miraculous off"));
    }

    private static void applyPassives(ServerPlayerEntity p, Hero h, int duration) {
        switch (h) {
            case LADYBUG -> { effect(p, StatusEffects.SPEED, duration, 1); effect(p, StatusEffects.JUMP_BOOST, duration, 1); effect(p, StatusEffects.RESISTANCE, duration, 0); }
            case CATNOIR -> { effect(p, StatusEffects.SPEED, duration, 1); effect(p, StatusEffects.JUMP_BOOST, duration, 2); effect(p, StatusEffects.NIGHT_VISION, duration, 0); effect(p, StatusEffects.STRENGTH, duration, 0); }
            case RENAROUGE -> { effect(p, StatusEffects.SPEED, duration, 1); effect(p, StatusEffects.JUMP_BOOST, duration, 1); effect(p, StatusEffects.NIGHT_VISION, duration, 0); }
            case CARAPACE -> { effect(p, StatusEffects.RESISTANCE, duration, 1); effect(p, StatusEffects.HASTE, duration, 1); effect(p, StatusEffects.SPEED, duration, 0); }
            case QUEENBEE -> { effect(p, StatusEffects.SPEED, duration, 1); effect(p, StatusEffects.SLOW_FALLING, duration, 0); effect(p, StatusEffects.JUMP_BOOST, duration, 1); }
            case VIPERION -> { effect(p, StatusEffects.SPEED, duration, 1); effect(p, StatusEffects.JUMP_BOOST, duration, 1); effect(p, StatusEffects.REGENERATION, duration, 0); }
            case PIGELLA -> { effect(p, StatusEffects.STRENGTH, duration, 0); effect(p, StatusEffects.RESISTANCE, duration, 0); effect(p, StatusEffects.SPEED, duration, 1); }
            case KINGMONKEY -> { effect(p, StatusEffects.STRENGTH, duration, 1); effect(p, StatusEffects.JUMP_BOOST, duration, 3); effect(p, StatusEffects.SPEED, duration, 1); }
            case RYUKO -> { effect(p, StatusEffects.SPEED, duration, 1); effect(p, StatusEffects.STRENGTH, duration, 0); effect(p, StatusEffects.FIRE_RESISTANCE, duration, 0); }
            case BUNNYX -> { effect(p, StatusEffects.SPEED, duration, 2); effect(p, StatusEffects.JUMP_BOOST, duration, 2); }
            case PEGASUS -> { effect(p, StatusEffects.SPEED, duration, 2); effect(p, StatusEffects.SLOW_FALLING, duration, 0); }
        }
    }

    private static void effect(ServerPlayerEntity p, RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect, int duration, int amplifier) {
        p.addStatusEffect(new StatusEffectInstance(effect, duration, amplifier, false, false, true));
    }

    private static void usePower(ServerPlayerEntity p) {
        ActiveHero active = ACTIVE.get(p.getUuid());
        if (active == null) { p.sendMessage(Text.literal("§cTransform first with /miraculous <hero>.")); return; }
        switch (active.hero) {
            case LADYBUG -> {
                p.heal(10.0F);
                effect(p, StatusEffects.REGENERATION, 20 * 10, 1);
                p.sendMessage(Text.literal("§c✦ Lucky Charm! §7You are healed and regenerated."));
            }
            case CATNOIR -> {
                effect(p, StatusEffects.STRENGTH, 20 * 15, 2);
                effect(p, StatusEffects.HASTE, 20 * 15, 1);
                p.sendMessage(Text.literal("§8✦ Cataclysm! §7Your strength surges briefly."));
            }
            case RENAROUGE -> {
                effect(p, StatusEffects.INVISIBILITY, 20 * 15, 0);
                p.sendMessage(Text.literal("§6✦ Mirage! §7You are invisible for 15 seconds."));
            }
            case CARAPACE -> {
                effect(p, StatusEffects.ABSORPTION, 20 * 20, 3);
                effect(p, StatusEffects.RESISTANCE, 20 * 15, 2);
                p.sendMessage(Text.literal("§a✦ Shell-ter! §7You are shielded."));
            }
            case QUEENBEE -> {
                effect(p, StatusEffects.SPEED, 20 * 8, 3);
                effect(p, StatusEffects.RESISTANCE, 20 * 8, 1);
                p.sendMessage(Text.literal("§e✦ Venom! §7You gain a burst of speed and protection."));
            }
            case VIPERION -> {
                effect(p, StatusEffects.REGENERATION, 20 * 12, 2);
                effect(p, StatusEffects.RESISTANCE, 20 * 12, 1);
                p.sendMessage(Text.literal("§3✦ Second Chance! §7You recover over the next few seconds."));
            }
            case PIGELLA -> {
                p.getHungerManager().setFoodLevel(20);
                p.getHungerManager().setSaturation(20.0F);
                effect(p, StatusEffects.REGENERATION, 20 * 10, 1);
                effect(p, StatusEffects.STRENGTH, 20 * 15, 1);
                p.sendMessage(Text.literal("§d✦ Piggy Power! §7You are fed and fired up."));
            }
            case KINGMONKEY -> {
                effect(p, StatusEffects.STRENGTH, 20 * 12, 2);
                effect(p, StatusEffects.SPEED, 20 * 12, 2);
                p.sendMessage(Text.literal("§4✦ Uproar! §7You gain a powerful burst."));
            }
            case RYUKO -> {
                effect(p, StatusEffects.SLOW_FALLING, 20 * 10, 0);
                effect(p, StatusEffects.SPEED, 20 * 8, 3);
                p.sendMessage(Text.literal("§c✦ Dragon Wind! §7You move swiftly and fall gently."));
            }
            case BUNNYX -> {
                effect(p, StatusEffects.SPEED, 20 * 10, 3);
                effect(p, StatusEffects.JUMP_BOOST, 20 * 10, 3);
                p.sendMessage(Text.literal("§6✦ Burrow! §7You gain a burst of speed and leaping."));
            }
            case PEGASUS -> {
                effect(p, StatusEffects.SLOW_FALLING, 20 * 20, 0);
                effect(p, StatusEffects.SPEED, 20 * 12, 2);
                p.sendMessage(Text.literal("§b✦ Voyage! §7You gain speed and slow falling."));
            }
        }
    }

    private static void detransform(ServerPlayerEntity p, String message) {
        ActiveHero old = ACTIVE.remove(p.getUuid());
        if (old == null) { p.sendMessage(Text.literal("§cYou're not transformed.")); return; }
        for (RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect : effectsFor(old.hero)) p.removeStatusEffect(effect);
        p.sendMessage(Text.literal(message));
    }

    private static List<RegistryEntry<net.minecraft.entity.effect.StatusEffect>> effectsFor(Hero h) {
        List<RegistryEntry<net.minecraft.entity.effect.StatusEffect>> list = new ArrayList<>();
        switch (h) {
            case LADYBUG -> list.addAll(List.of(StatusEffects.SPEED, StatusEffects.JUMP_BOOST, StatusEffects.RESISTANCE));
            case CATNOIR -> list.addAll(List.of(StatusEffects.SPEED, StatusEffects.JUMP_BOOST, StatusEffects.NIGHT_VISION, StatusEffects.STRENGTH));
            case RENAROUGE -> list.addAll(List.of(StatusEffects.SPEED, StatusEffects.JUMP_BOOST, StatusEffects.NIGHT_VISION));
            case CARAPACE -> list.addAll(List.of(StatusEffects.RESISTANCE, StatusEffects.HASTE, StatusEffects.SPEED));
            case QUEENBEE -> list.addAll(List.of(StatusEffects.SPEED, StatusEffects.SLOW_FALLING, StatusEffects.JUMP_BOOST));
            case VIPERION -> list.addAll(List.of(StatusEffects.SPEED, StatusEffects.JUMP_BOOST, StatusEffects.REGENERATION));
            case PIGELLA -> list.addAll(List.of(StatusEffects.STRENGTH, StatusEffects.RESISTANCE, StatusEffects.SPEED));
            case KINGMONKEY -> list.addAll(List.of(StatusEffects.STRENGTH, StatusEffects.JUMP_BOOST, StatusEffects.SPEED));
            case RYUKO -> list.addAll(List.of(StatusEffects.SPEED, StatusEffects.STRENGTH, StatusEffects.FIRE_RESISTANCE));
            case BUNNYX -> list.addAll(List.of(StatusEffects.SPEED, StatusEffects.JUMP_BOOST));
            case PEGASUS -> list.addAll(List.of(StatusEffects.SPEED, StatusEffects.SLOW_FALLING));
        }
        return list;
    }

    private static void tick(MinecraftServer server) {
        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, ActiveHero> entry : ACTIVE.entrySet()) {
            ActiveHero active = entry.getValue();
            active.ticksLeft--;
            if (active.ticksLeft <= 0) expired.add(entry.getKey());
        }
        for (UUID id : expired) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
            ActiveHero old = ACTIVE.remove(id);
            if (p != null && old != null) {
                for (RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect : effectsFor(old.hero)) p.removeStatusEffect(effect);
                p.sendMessage(Text.literal("§7Your Miraculous timer ran out!"));
            }
        }
    }
}
