// HOBPaymentsAPI.java
package com.cleardragonf.asura.hobpayments.api;

import com.cleardragonf.asura.HOB;
import com.cleardragonf.asura.hobpayments.economy.EconomyManager;

public class HOBPaymentsAPI {
    private static final EconomyManager economyManager = HOB.economyManager;

    public static void setBalance(String playerName, double amount) {
        economyManager.setBalance(playerName, amount);
    }

    public static double getBalance(String playerName) {
        return economyManager.getBalance(playerName);
    }

    public static void addBalance(String playerName, double amount) {
        economyManager.addBalance(playerName, amount);
    }

    public static void subtractBalance(String playerName, double amount) {
        economyManager.subtractBalance(playerName, amount);
    }
}
