package com.cleardragonf.asura.utilities;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Formatting {

    public static String formatAsCurrency(BigDecimal amount) {
        return "$" + amount.setScale(2, RoundingMode.HALF_UP).toString();
    }
}
