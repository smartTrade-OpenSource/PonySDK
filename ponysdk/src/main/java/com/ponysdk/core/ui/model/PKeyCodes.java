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

package com.ponysdk.core.ui.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains the native key codes.
 */
public enum PKeyCodes {

    A(65),
    B(66),
    C(67),
    D(68),
    E(69),
    F(70),
    G(71),
    H(72),
    I(73),
    J(74),
    K(75),
    L(76),
    M(77),
    N(78),
    O(79),
    P(80),
    Q(81),
    R(82),
    S(83),
    T(84),
    U(85),
    V(86),
    W(87),
    X(88),
    Y(89),
    Z(90),

    ZERO(48),
    ONE(49),
    TWO(50),
    THREE(51),
    FOUR(52),
    FIVE(53),
    SIX(54),
    SEVEN(55),
    EIGHT(56),
    NINE(57),

    NUM_CENTER(12), // Also NUM_LOCK
    NUM_ZERO(96),
    NUM_ONE(97),
    NUM_TWO(98),
    NUM_THREE(99),
    NUM_FOUR(100),
    NUM_FIVE(101),
    NUM_SIX(102),
    NUM_SEVEN(103),
    NUM_EIGHT(104),
    NUM_NINE(105),
    NUM_MULTIPLY(106),
    NUM_PLUS(107),
    NUM_MINUS(109),
    NUM_PERIOD(110),
    NUM_DIVISION(111),

    BACKSPACE(8),
    ENTER(13),
    MAC_ENTER(3),
    ESCAPE(27),

    DELETE(46),
    HOME(36),
    END(35),
    PAGEUP(33),
    PAGEDOWN(34),

    TAB(9),
    SHIFT(16),
    CTRL(17),
    ALT(18),
    CAPS_LOCK(20),

    LEFT(37),
    UP(38),
    RIGHT(39),
    DOWN(40),

    F1(112),
    F2(113),
    F3(114),
    F4(115),
    F5(116),
    F6(117),
    F7(118),
    F8(119),
    F9(120),
    F10(121),
    F11(122),
    F12(123),

    WIN_KEY_FF_LINUX(0),

    INSERT(45), // also NUM_INSERT
    PRINT_SCREEN(44),
    PAUSE(19),

    SPACE(32),

    WIN_KEY(224),
    WIN_KEY_RIGHT(92),

    KEY_WIN_KEY_LEFT_META(91),
    MAC_FF_META(224), // Firefox (Gecko) fires this for the meta key instead of 91

    CONTEXT_MENU(93),

    NUMLOCK(144),
    SCROLL_LOCK(145),

    FIRST_MEDIA_KEY(166),
    LAST_MEDIA_KEY(183),

    WIN_IME(229);

    private static Map<Integer, PKeyCodes> codesByKey = new HashMap<>();

    static {
        for (final PKeyCodes code : PKeyCodes.values()) {
            codesByKey.put(code.getCode(), code);
        }
    }

    private final int code;

    PKeyCodes(final int code) {
        this.code = code;
    }

    public static PKeyCodes fromInt(final int code) {
        return codesByKey.get(code);
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
}
