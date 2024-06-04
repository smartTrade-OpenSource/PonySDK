package com.ponysdk.core.ui.listbox;

import java.util.HashMap;
import java.util.Map;

public class KeyCodeUtil {

    private static final Map<Integer, String> KEY_CODES = new HashMap<>();

    static {
        KEY_CODES.put(48, "0");
        KEY_CODES.put(49, "1");
        KEY_CODES.put(50, "2");
        KEY_CODES.put(51, "3");
        KEY_CODES.put(52, "4");
        KEY_CODES.put(53, "5");
        KEY_CODES.put(54, "6");
        KEY_CODES.put(55, "7");
        KEY_CODES.put(56, "8");
        KEY_CODES.put(57, "9");
        KEY_CODES.put(65, "A");
        KEY_CODES.put(66, "B");
        KEY_CODES.put(67, "C");
        KEY_CODES.put(68, "D");
        KEY_CODES.put(69, "E");
        KEY_CODES.put(70, "F");
        KEY_CODES.put(71, "G");
        KEY_CODES.put(72, "H");
        KEY_CODES.put(73, "I");
        KEY_CODES.put(74, "J");
        KEY_CODES.put(75, "K");
        KEY_CODES.put(76, "L");
        KEY_CODES.put(77, "M");
        KEY_CODES.put(78, "N");
        KEY_CODES.put(79, "O");
        KEY_CODES.put(80, "P");
        KEY_CODES.put(81, "Q");
        KEY_CODES.put(82, "R");
        KEY_CODES.put(83, "S");
        KEY_CODES.put(84, "T");
        KEY_CODES.put(85, "U");
        KEY_CODES.put(86, "V");
        KEY_CODES.put(87, "W");
        KEY_CODES.put(88, "X");
        KEY_CODES.put(89, "Y");
        KEY_CODES.put(90, "Z");
        KEY_CODES.put(96, "0");
        KEY_CODES.put(97, "1");
        KEY_CODES.put(98, "2");
        KEY_CODES.put(99, "3");
        KEY_CODES.put(100, "4");
        KEY_CODES.put(101, "5");
        KEY_CODES.put(102, "6");
        KEY_CODES.put(103, "7");
        KEY_CODES.put(104, "8");
        KEY_CODES.put(105, "9");
    }

    public static String getString(int jsKeyCode) {
        return KEY_CODES.get(jsKeyCode);
    }
}
