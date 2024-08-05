package com.cleardragonf.asura.hobpayments.economy;

import com.cleardragonf.asura.hobpayments.config.ModConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EconomyManager {
    private final Map<String, Double> playerBalances = new HashMap<>();

    public EconomyManager() {
        loadBalancesFromConfig();
    }

    public void setBalance(String playerName, double amount) {
        playerBalances.put(playerName, amount);
        saveBalancesToConfig();
    }

    public double getBalance(String playerName) {
        return playerBalances.getOrDefault(playerName, 0.0);
    }

    public void addBalance(String playerName, double amount) {
        double currentBalance = getBalance(playerName);
        setBalance(playerName, currentBalance + amount);
    }

    public void subtractBalance(String playerName, double amount) {
        double currentBalance = getBalance(playerName);
        setBalance(playerName, currentBalance - amount);
    }

    private void loadBalancesFromConfig() {
        List<? extends String> balanceList = ModConfig.COMMON.playerBalances.get();
        for (String entry : balanceList) {
            String[] parts = entry.split(":");
            if (parts.length == 2) {
                String playerName = parts[0];
                double balance = Double.parseDouble(parts[1]);
                playerBalances.put(playerName, balance);
            }
        }
    }

    private void saveBalancesToConfig() {
        List<String> balanceList = playerBalances.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.toList());
        ModConfig.COMMON.playerBalances.set(balanceList);
    }
}
