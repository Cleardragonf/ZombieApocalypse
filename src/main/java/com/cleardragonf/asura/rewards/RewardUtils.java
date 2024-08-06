package com.cleardragonf.asura.rewards;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class RewardUtils {

    public static BigDecimal getRandomReward(BigDecimal minValue, BigDecimal maxValue) {
        BigDecimal range = maxValue.subtract(minValue);
        BigDecimal randomValue = minValue.add(range.multiply(BigDecimal.valueOf(Math.random())));
        return randomValue.setScale(2, RoundingMode.HALF_UP);
    }
}
