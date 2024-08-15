package com.cleardragonf.asura.commands;

import com.cleardragonf.asura.HOB;
import com.cleardragonf.asura.hobpayments.economy.EconomyManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class HOBCommands {

    private static final double RESET_COST = 500.0; // Cost to execute the reset command

    // Map to define costs for each entity type
    private static final Map<ResourceLocation, Double> ENTITY_COSTS = new HashMap<>();

    static {
        // Define costs for each entity type
        ENTITY_COSTS.put(new ResourceLocation("minecraft:zombie"), 50.0);
        ENTITY_COSTS.put(new ResourceLocation("minecraft:skeleton"), 50.0);
        ENTITY_COSTS.put(new ResourceLocation("minecraft:creeper"), 50.0);
        // Add more entities and their costs here
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("HOB")
                .then(Commands.literal("reset")
                        .executes(context -> executeReset(context)))
                .then(Commands.literal("hit")
                        .then(Commands.argument("player", EntityArgument.player())
                                .suggests((context, builder) -> suggestSpecificPlayers(context, builder))
                                .executes(context -> {
                                    // Command logic here
                                    return 1;
                                })
                                .then(Commands.argument("mob", StringArgumentType.greedyString()) // Using greedyString here
                                        .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                                ForgeRegistries.ENTITY_TYPES.getKeys().stream()
                                                        .map(ResourceLocation::toString)
                                                        .collect(Collectors.toList()), builder))
                                        .executes(context -> executeHit(context, StringArgumentType.getString(context, "player"), StringArgumentType.getString(context, "mob"))))))
                .then(Commands.literal("day")
                        .executes(context -> executeDay(context)))
                .then(Commands.literal("clean")
                        .executes(context -> executeClean(context)))
        );
    }

    private static CompletableFuture<Suggestions> suggestSpecificPlayers(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        // Get a list of specific players to suggest
        Collection<ServerPlayer> players = context.getSource().getServer().getPlayerList().getPlayers();
        Collection<String> playerNames = players.stream()
                .filter(player -> /* Apply any filtering logic here */ true)
                .map(ServerPlayer::getName)
                .map(Object::toString)
                .collect(Collectors.toList());

        // Use SharedSuggestionProvider to suggest player names
        return SharedSuggestionProvider.suggest(playerNames, builder);
    }

    private static int executeReset(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Command can only be executed by a player."));
            return 0;
        }

        EconomyManager eco = HOB.economyManager;
        double balance = eco.getBalance(player.getName().getString());

        if (balance < RESET_COST) {
            source.sendFailure(Component.literal("You do not have enough to execute this command"));
            return 0;
        }

        eco.subtractBalance(player.getName().getString(), RESET_COST);
        HOB.wipeEntitiesFromHOBSpawned();
        source.sendSuccess(() -> Component.literal("You have removed the HOB Wave"), true);
        return 1;
    }

    private static int executeClean(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Command can only be executed by a player."));
            return 0;
        }

        double COST = 500;
        EconomyManager eco = HOB.economyManager;
        double balance = eco.getBalance(player.getName().getString());

        if (balance < COST) {
            source.sendFailure(Component.literal("You do not have enough to execute this command"));
            return 0;
        }

        HOB.removeBlocksFromGame(source.getLevel());
        source.sendSuccess(() -> Component.literal("You have executed the clean command"), true);
        return 1;
    }

    private static int executeHit(CommandContext<CommandSourceStack> context, String playerName, String mobName) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Command can only be executed by a player."));
            return 0;
        }

        EconomyManager eco = HOB.economyManager;
        double balance = eco.getBalance(player.getName().getString());
        //System.out.println(balance);

        ResourceLocation mobResourceLocation = new ResourceLocation(mobName);
        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(mobResourceLocation);
        if (entityType == null) {
            source.sendFailure(Component.literal("Invalid mob type specified."));
            return 0;
        }

        // Retrieve the cost for the specified entity
        Double entityCost = ENTITY_COSTS.get(mobResourceLocation);
        if (entityCost == null) {
            source.sendFailure(Component.literal("No cost defined for this entity."));
            return 0;
        }

        if (balance < entityCost) {
            source.sendFailure(Component.literal("You do not have enough to execute this command"));
            return 0;
        }

        eco.subtractBalance(player.getName().getString(), entityCost);

        ServerLevel world = source.getLevel();
        ServerPlayer targetPlayer = world.getServer().getPlayerList().getPlayerByName(playerName);
        if (targetPlayer == null) {
            source.sendFailure(Component.literal("Player not found."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("You have sent a " + mobName + " after " + playerName), true);
        return 1;
    }

    private static int executeDay(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("Current Day is: " + HOB.currentDay), true);
        return 1;
    }
}
