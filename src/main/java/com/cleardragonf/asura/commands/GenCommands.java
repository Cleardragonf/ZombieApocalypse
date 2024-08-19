package com.cleardragonf.asura.commands;

import com.cleardragonf.asura.HOB;
import com.cleardragonf.asura.hobpayments.economy.EconomyManager;
import com.cleardragonf.asura.utilities.DeathTracking;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.cleardragonf.asura.commands.HomeData;
import org.w3c.dom.Text;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class GenCommands {

    private static final double COST = 20.0; // Cost to teleport to last death location
    private static final int COUNTDOWN_NO_MONEY = 10; // 10 seconds if no money
    private static final int COUNTDOWN_WITH_MONEY = 2; // 2 seconds if enough money

    private static final Map<String, Map<String, HomeData>> playerHomes = new HashMap<>();
    private static final File DATA_FILE = new File("config/player_homes.json"); // File to store home data

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        loadHomeData(); // Load home data on startup

        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("back")
                .executes(context -> executeBack(context))
        );
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("sethome")
                .then(Commands.argument("homeName", StringArgumentType.word())
                        .then(Commands.argument("instant", BoolArgumentType.bool()).executes(context -> executeSetHome(context, BoolArgumentType.getBool(context, "instant"))))
                        .executes(context -> executeSetHome(context, false)))
        );
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("delhome")
                .then(Commands.argument("homeName", StringArgumentType.word())
                        .executes(context -> executeDelHome(context)))
        );
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("home")
                .then(Commands.argument("homeName", StringArgumentType.word())
                        .executes(context -> executeHome(context)))
        );
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("listhomes")
                .executes(GenCommands::executeListHomes)
        );

    }

    private static int executeBack(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (source.getEntity() instanceof ServerPlayer player) {
            EconomyManager eco = HOB.economyManager;
            String playerName = player.getGameProfile().getName();
            double balance = eco.getBalance(playerName);
            int countdown = balance >= COST ? COUNTDOWN_WITH_MONEY : COUNTDOWN_NO_MONEY;

            if (balance < COST) {
                source.sendFailure(Component.literal("You do not have enough money to go back to the place of your death. Starting a 10-second countdown..."));
            } else {
                eco.subtractBalance(playerName, COST);
                source.sendSuccess(() ->Component.literal("You have enough money. Starting a 2-second countdown..."), true);
            }

            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(countdown * 1000); // Countdown
                    Vec3 lastDeathLocation = DeathTracking.getLastDeathLocation(player);
                    if (lastDeathLocation != null) {
                        player.teleportTo(lastDeathLocation.x, lastDeathLocation.y, lastDeathLocation.z);
                        source.sendSuccess(() -> Component.literal("Teleported to last death location."), true);
                    } else {
                        source.sendFailure(Component.literal("No death location found."));
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        return 1;
    }


    private static int executeSetHome(CommandContext<CommandSourceStack> context, boolean instant) {
        CommandSourceStack source = context.getSource();
        if (source.getEntity() instanceof ServerPlayer player) {
            String playerName = player.getGameProfile().getName();
            String homeName = StringArgumentType.getString(context, "homeName");
            Vec3 currentLocation = player.position();
            ResourceKey<Level> currentDimension = player.level().dimension(); // Correctly get the ResourceKey<Level>
            double BASE_COST = 10.0; // Base cost for setting a home
            double INSTANT_MULTIPLIER = 1.5; // Multiplier for instant teleportation cost

            // Get or create the player's home map
            Map<String, HomeData> homes = playerHomes.computeIfAbsent(playerName, k -> new HashMap<>());

            // Calculate cost based on the number of homes
            int numberOfHomes = homes.size();
            double cost = BASE_COST * Math.pow(2, numberOfHomes); // Exponential cost based on the number of homes

            // Apply instant teleportation multiplier if applicable
            if (instant) {
                cost *= INSTANT_MULTIPLIER;
            }

            EconomyManager eco = HOB.economyManager;
            double balance = eco.getBalance(playerName);

            if (balance < cost) {
                source.sendFailure(Component.literal("You do not have enough money to set a new home. You need " + cost + " coins."));
                return 0; // Not enough money
            }

            // Deduct the cost
            eco.subtractBalance(playerName, cost);

            // Store the home location and dimension
            homes.put(homeName, new HomeData(currentLocation, currentDimension)); // Pass ResourceKey<Level> directly
            saveHomeData(); // Save data after modification
            double finalCost = cost;
            source.sendSuccess(() -> Component.literal("Home '" + homeName + "' set at your current location in " + currentDimension + ". Cost: " + finalCost + " coins."), true);

            if (!instant) {
                source.sendSuccess(() -> Component.literal("This home will have a 2-second countdown when used."), true);
            } else {
                source.sendSuccess(() -> Component.literal("This home will allow instant teleportation."), true);
            }
        }
        return 1;
    }

    private static int executeDelHome(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (source.getEntity() instanceof ServerPlayer player) {
            String playerName = player.getGameProfile().getName();
            String homeName = StringArgumentType.getString(context, "homeName");

            // Get the player's home map
            Map<String, HomeData> homes = playerHomes.get(playerName);

            if (homes != null && homes.containsKey(homeName)) {
                homes.remove(homeName);
                saveHomeData(); // Save data after modification
                source.sendSuccess(() -> Component.literal("Home '" + homeName + "' deleted."), true);
            } else {
                source.sendFailure(Component.literal("No home found with the name '" + homeName + "'."));
            }
        }
        return 1;
    }


    private static int executeHome(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (source.getEntity() instanceof ServerPlayer player) {
            String playerName = player.getGameProfile().getName();
            String homeName = StringArgumentType.getString(context, "homeName");
            double BASE_COST = 10.0; // Base cost per 10 chunks
            int COUNTDOWN_WITH_MONEY = 2; // Countdown in seconds when the player has enough money

            // Get the player's home map
            Map<String, HomeData> homes = playerHomes.get(playerName);

            if (homes != null && homes.containsKey(homeName)) {
                HomeData homeData = homes.get(homeName);
                Vec3 homeLocation = homeData.getPosition();
                ResourceKey<Level> homeDimension = homeData.getDimension();
                Vec3 currentLocation = player.position();

                // Calculate the distance in blocks
                double distance = currentLocation.distanceTo(homeLocation);

                // Calculate the cost based on the distance (in chunks of 10)
                double chunkDistance = distance / 160.0;
                double cost = BASE_COST * chunkDistance;

                EconomyManager eco = HOB.economyManager;
                double balance = eco.getBalance(playerName);

                if (balance < cost) {
                    int countdown = 10; // Default 10-second countdown if the player doesn't have enough money
                    source.sendSuccess(() -> Component.literal("You don't have enough money. Starting 10-second countdown before teleportation..."), true);

                    CompletableFuture.runAsync(() -> {
                        try {
                            Thread.sleep(countdown * 1000); // Countdown
                            ServerLevel targetLevel = player.getServer().getLevel(homeDimension);
                            if (targetLevel != null) {
                                player.teleportTo(targetLevel, homeLocation.x, homeLocation.y, homeLocation.z, player.getYRot(), player.getXRot());
                                source.sendSuccess(() -> Component.literal("Teleported to home '" + homeName + "'."), true);
                            } else {
                                source.sendFailure(Component.literal("Cannot find the dimension for home '" + homeName + "'."));
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    });
                } else {
                    // Deduct the cost
                    eco.subtractBalance(playerName, cost);
                    source.sendSuccess(() -> Component.literal("Starting teleportation countdown..."), true);

                    int countdown = COUNTDOWN_WITH_MONEY;
                    CompletableFuture.runAsync(() -> {
                        try {
                            Thread.sleep(countdown * 1000); // Countdown
                            ServerLevel targetLevel = player.getServer().getLevel(homeDimension);
                            if (targetLevel != null) {
                                player.teleportTo(targetLevel, homeLocation.x, homeLocation.y, homeLocation.z, player.getYRot(), player.getXRot());
                                source.sendSuccess(() -> Component.literal("Teleported to home '" + homeName + "'. Cost: " + cost + " coins."), true);
                            } else {
                                source.sendFailure(Component.literal("Cannot find the dimension for home '" + homeName + "'."));
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    });
                }
            } else {
                source.sendFailure(Component.literal("No home found with the name '" + homeName + "'."));
            }
        }
        return 1;
    }

    private static int executeListHomes(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (source.getEntity() instanceof ServerPlayer player) {
            String playerName = player.getGameProfile().getName();
            Vec3 currentLocation = player.position();

            // Get the player's home map
            Map<String, HomeData> homes = playerHomes.get(playerName);

            if (homes != null && !homes.isEmpty()) {
                Component homeList = Component.literal("Your saved homes:\n");
                for (Map.Entry<String, HomeData> entry : homes.entrySet()) {
                    String homeName = entry.getKey();
                    HomeData homeData = entry.getValue();

                    if (homeData != null) {
                        Vec3 homeLocation = homeData.getPosition();

                        if (homeLocation != null) {
                            double distance = currentLocation.distanceTo(homeLocation);
                            double chunkDistance = Math.ceil(distance / 160.0);
                            double cost = 10 + (chunkDistance * 10);

                            // Create clickable text for each home
                            Component homeText = Component.literal(homeName)
                                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x00FF00)) // Use RGB for green
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/home " + homeName))
                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Teleport to " + homeName))))
                                    .append(Component.literal(" - " + cost + " units\n"));

                            // Append to the list of homes
                            homeList = ((MutableComponent) homeList).append(homeText);
                        } else {
                            source.sendFailure(Component.literal("Home location for '" + homeName + "' is not set properly."));
                        }
                    } else {
                        source.sendFailure(Component.literal("Home data for '" + homeName + "' is missing."));
                    }
                }
                Component finalHomeList = homeList;
                source.sendSuccess(() -> finalHomeList, false);
            } else {
                source.sendFailure(Component.literal("You have no saved homes."));
            }
        }
        return 1;
    }

    private static void saveHomeData() {
        try (FileWriter writer = new FileWriter(DATA_FILE)) {
            Gson gson = new Gson();
            gson.toJson(playerHomes, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadHomeData() {
        if (!DATA_FILE.exists()) {
            return; // No data to load
        }
        try (FileReader reader = new FileReader(DATA_FILE)) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Map<String, HomeData>>>() {}.getType();
            Map<String, Map<String, HomeData>> data = gson.fromJson(reader, type);
            if (data != null) {
                playerHomes.clear();
                playerHomes.putAll(data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
