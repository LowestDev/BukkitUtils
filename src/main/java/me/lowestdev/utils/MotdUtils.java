package me.lowestdev.utils;

import java.util.HashMap;
import java.util.Map;

public class MotdUtils {

    private static final Map<Character, Integer> CHAR_WIDTHS = new HashMap<>();

    static {
        CHAR_WIDTHS.put(' ', 4);
        CHAR_WIDTHS.put('i', 2);
        CHAR_WIDTHS.put('l', 3);
        CHAR_WIDTHS.put('t', 4);
        CHAR_WIDTHS.put('f', 4);
        CHAR_WIDTHS.put('r', 5);
        CHAR_WIDTHS.put('k', 5);
        CHAR_WIDTHS.put('s', 5);
        CHAR_WIDTHS.put('x', 5);
        CHAR_WIDTHS.put('y', 5);
        CHAR_WIDTHS.put('z', 5);
        CHAR_WIDTHS.put('I', 4);
        CHAR_WIDTHS.put('J', 5);
    }

    public static String centerMotd(String text) {
        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;

        for (char c : text.toCharArray()) {
            if (c == '§') {
                previousCode = true;
                continue;
            } else if (previousCode) {
                previousCode = false;
                isBold = (c == 'l' || c == 'L');
                continue;
            }

            int charWidth = CHAR_WIDTHS.getOrDefault(c, 6); // default char width
            if (isBold) charWidth++;
            messagePxSize += charWidth + 1; // +1 pixel space
        }

        int CENTER_PX = 154;
        int halvedMessageSize = messagePxSize / 2;
        int toCompensate = CENTER_PX - halvedMessageSize;

        int spaceWidth = CHAR_WIDTHS.get(' ');
        int compensated = 0;
        StringBuilder sb = new StringBuilder();
        while (compensated < toCompensate) {
            sb.append(" ");
            compensated += spaceWidth + 1;
        }
        return sb.toString() + text;
    }
}
