package com.cleardragonf.asura.hobpayments.commands;

import com.cleardragonf.asura.HOB;
import com.cleardragonf.asura.utilities.Formatting;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.cleardragonf.asura.hobpayments.economy.EconomyManager;

import java.math.BigDecimal;

public class EconomyCommands {
    private static final EconomyManager economyManager = HOB.economyManager;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("balance")
                .executes(context -> {
                    String playerName = context.getSource().getPlayerOrException().getName().getString();
                    double balance = economyManager.getBalance(playerName);
                    Component message = Component.literal("Your balance is: " + Formatting.formatAsCurrency(BigDecimal.valueOf(balance)));
                    context.getSource().sendSuccess(() -> message, false);
                    return Command.SINGLE_SUCCESS;
                }));

        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("pay")
                .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                .executes(context -> {
                                    double amount = DoubleArgumentType.getDouble(context, "amount");
                                    String payerName = context.getSource().getPlayerOrException().getName().getString();
                                    String recipientName = StringArgumentType.getString(context, "player");

                                    // Ensure the payer has enough balance
                                    double payerBalance = economyManager.getBalance(payerName);
                                    if (payerBalance < amount) {
                                        Component message = Component.literal("Insufficient balance.");
                                        context.getSource().sendSuccess(() -> message, false);
                                        return Command.SINGLE_SUCCESS;
                                    }

                                    // Perform the transaction
                                    economyManager.addBalance(recipientName, amount);
                                    economyManager.subtractBalance(payerName, amount);

                                    // Notify both payer and recipient
                                    Component payerMessage = Component.literal("Paid: " + Formatting.formatAsCurrency(BigDecimal.valueOf(amount)) + " to " + recipientName);
                                    context.getSource().sendSuccess(() -> payerMessage, false);

                                    ServerPlayer recipient = context.getSource().getServer().getPlayerList().getPlayerByName(recipientName);
                                    if (recipient != null) {
                                        Component recipientMessage = Component.literal("Received: " + Formatting.formatAsCurrency(BigDecimal.valueOf(amount)) + " from " + payerName);
                                        recipient.sendSystemMessage(recipientMessage);
                                    } else {
                                        // Handle case where recipient is not online
                                        Component message = Component.literal("Player " + recipientName + " is not online.");
                                        context.getSource().sendSuccess(() -> message, false);
                                    }

                                    return Command.SINGLE_SUCCESS;
                                }))));

        // Command to set player balance, restricted to console only
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("set")
                .requires(source -> source.getEntity() == null) // Ensure the command is run from the console
                .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                .executes(context -> {
                                    String playerName = StringArgumentType.getString(context, "player");
                                    double amount = DoubleArgumentType.getDouble(context, "amount");
                                    economyManager.setBalance(playerName, amount);
                                    Component message = Component.literal("Set balance of " + playerName + " to: " + Formatting.formatAsCurrency(BigDecimal.valueOf(amount)));
                                    context.getSource().sendSuccess(() -> message, false);
                                    return Command.SINGLE_SUCCESS;
                                }))));
    }
}