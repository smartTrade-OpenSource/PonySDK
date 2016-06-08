/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.core.ui.basic;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains the native key codes.
 */
public enum PKeyCodes {
    /**
     * Key code for A
     */
    A(65),
    /**
     * Key code for B
     */
    B(66),
    /**
     * Key code for C
     */
    C(67),
    /**
     * Key code for D
     */
    D(68),
    /**
     * Key code for E
     */
    E(69),
    /**
     * Key code for F
     */
    F(70),
    /**
     * Key code for G
     */
    G(71),
    /**
     * Key code for H
     */
    H(72),
    /**
     * Key code for I
     */
    I(73),
    /**
     * Key code for J
     */
    J(74),
    /**
     * Key code for K
     */
    K(75),
    /**
     * Key code for L
     */
    L(76),
    /**
     * Key code for M
     */
    M(77),
    /**
     * Key code for N
     */
    N(78),
    /**
     * Key code for O
     */
    O(79),
    /**
     * Key code for P
     */
    P(80),
    /**
     * Key code for Q
     */
    Q(81),
    /**
     * Key code for R
     */
    R(82),
    /**
     * Key code for S
     */
    S(83),
    /**
     * Key code for T
     */
    T(84),
    /**
     * Key code for U
     */
    U(85),
    /**
     * Key code for V
     */
    V(86),
    /**
     * Key code for W
     */
    W(87),
    /**
     * Key code for X
     */
    X(88),
    /**
     * Key code for Y
     */
    Y(89),
    /**
     * Key code for Z
     */
    Z(90),

    /**
     * Key code number 0
     */
    ZERO(48),
    /**
     * Key code number 1
     */
    ONE(49),
    /**
     * Key code number 2
     */
    TWO(50),
    /**
     * Key code number 3
     */
    THREE(51),
    /**
     * Key code number 4
     */
    FOUR(52),
    /**
     * Key code number 5
     */
    FIVE(53),
    /**
     * Key code number 6
     */
    SIX(54),
    /**
     * Key code number 7
     */
    SEVEN(55),
    /**
     * Key code number 8
     */
    EIGHT(56),
    /**
     * Key code number 9
     */
    NINE(57),

    /**
     * Key code for number 0 on numeric keyboard
     */
    NUM_ZERO(96),
    /**
     * Key code for number 1 on numeric keyboard
     */
    NUM_ONE(97),
    /**
     * Key code for number 2 on numeric keyboard
     */
    NUM_TWO(98),
    /**
     * Key code for number 3 on numeric keyboard
     */
    NUM_THREE(99),
    /**
     * Key code for number 4 on numeric keyboard
     */
    NUM_FOUR(100),
    /**
     * Key code for number 5 on numeric keyboard
     */
    NUM_FIVE(101),
    /**
     * Key code for number 6 on numeric keyboard
     */
    NUM_SIX(102),
    /**
     * Key code for number 7 on numeric keyboard
     */
    NUM_SEVEN(103),
    /**
     * Key code for number 8 on numeric keyboard
     */
    NUM_EIGHT(104),
    /**
     * Key code for number 9 on numeric keyboard
     */
    NUM_NINE(105),
    /**
     * Key code for multiply on numeric keyboard
     */
    NUM_MULTIPLY(106),
    /**
     * Key code for plus on numeric keyboard
     */
    NUM_PLUS(107),
    /**
     * Key code for minus on numeric keyboard
     */
    NUM_MINUS(109),
    /**
     * Key code for period on numeric keyboard
     */
    NUM_PERIOD(110),
    /**
     * Key code for division on numeric keyboard
     */
    NUM_DIVISION(111),
    /**
     * Alt key code.
     */
    ALT(18),

    /**
     * Backspace key code.
     */
    BACKSPACE(8),
    /**
     * Control key code.
     */
    CTRL(17),

    /**
     * Delete key code (also numeric keypad delete).
     */
    DELETE(46),

    /**
     * Down arrow code (Also numeric keypad down).
     */
    DOWN(40),

    /**
     * End key code (Also numeric keypad south west).
     */
    END(35),

    /**
     * Enter key code.
     */
    ENTER(13),
    /**
     * Escape key code.
     */
    ESCAPE(27),
    /**
     * Home key code (Also numeric keypad north west).
     */
    HOME(36),
    /**
     * Left key code (Also numeric keypad west).
     */
    LEFT(37),
    /**
     * Page down key code (Also numeric keypad south east).
     */
    PAGEDOWN(34),
    /**
     * Page up key code (Also numeric keypad north east).
     */
    PAGEUP(33),
    /**
     * Right arrow key code (Also numeric keypad east).
     */
    RIGHT(39),
    /**
     * Shift key code.
     */
    SHIFT(16),

    /**
     * Tab key code.
     */
    TAB(9),
    /**
     * Up Arrow key code (Also numeric keypad north).
     */
    UP(38),

    /**
     * Key code for F1
     */
    F1(112),
    /**
     * Key code for F2
     */
    F2(113),
    /**
     * Key code for F3
     */
    F3(114),
    /**
     * Key code for F4
     */
    F4(115),
    /**
     * Key code for F5
     */
    F5(116),
    /**
     * Key code for F6
     */
    F6(117),
    /**
     * Key code for F7
     */
    F7(118),
    /**
     * Key code for F8
     */
    F8(119),
    /**
     * Key code for F9
     */
    F9(120),
    /**
     * Key code for F10
     */
    F10(121),
    /**
     * Key code for F11
     */
    F11(122),
    /**
     * Key code for F12
     */
    F12(123),
    /**
     * Key code for Windows key on Firefox Linux
     */
    WIN_KEY_FF_LINUX(0),
    /**
     * Key code for Mac enter key
     */
    MAC_ENTER(3),
    /**
     * Key code for pause key
     */
    PAUSE(19),
    /**
     * Key code for caps lock key
     */
    CAPS_LOCK(20),
    /**
     * Key code for space
     */
    SPACE(32),

    /**
     * Key code for print key
     */
    PRINT_SCREEN(44),
    /**
     * Key code for insert key (Also numeric keyboard insert).
     */
    INSERT(45), // also NUM_INSERT

    /**
     * Key code for insert key (Also num lock on FF,Safari Mac).
     */
    NUM_CENTER(12),

    /**
     * Key code for left windows key.
     */
    WIN_KEY(224),

    /**
     * Key code for left windows key or meta.
     */

    KEY_WIN_KEY_LEFT_META(91),

    /**
     * Key code for right windows key.
     */

    WIN_KEY_RIGHT(92),
    /**
     * Key code for context menu key.
     */

    CONTEXT_MENU(93),
    /**
     * Key code for {@link KeyCodes#,WIN_,LEFT_META} that Firefox fires for the meta key.
     */
    MAC_FF_META(224), // Firefox (Gecko) fires this for the meta key instead of 91

    /**
     * Key code for num lock.
     */
    NUMLOCK(144),
    /**
     * Key code for scroll lock.
     */
    SCROLL_LOCK(145),

    /**
     * Key code for first OS specific media key (like volume).
     */
    FIRST_MEDIA_KEY(166),
    /**
     * Key code for last OS specific media key (like volume).
     */
    LAST_MEDIA_KEY(183),
    /**
     * Key code for IME.
     */

    WIN_IME(229);

    private static Map<Integer, PKeyCodes> codesByKey;

    static {
        codesByKey = new HashMap<>();
        for (final PKeyCodes code : PKeyCodes.values()) {
            codesByKey.put(code.getCode(), code);
        }
    }

    private int code;

    PKeyCodes(final int code) {
        this.code = code;
    }

    public boolean equals(final int code) {
        return this.code == code;
    }

    public int getCode() {
        return this.code;
    }

    public String getCodeToString() {
        return this.code + "";
    }

    public static PKeyCodes fromInt(final int code) {
        return codesByKey.get(code);
    }
}
