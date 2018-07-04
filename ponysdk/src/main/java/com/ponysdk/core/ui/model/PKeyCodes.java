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
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/KeyboardEvent/keyCode">MDN</a>
 */
public enum PKeyCodes {

    UNKNOWN(-1),

    WIN_KEY_FF_LINUX(0),

    CANCEL(3), // also MAC_ENTER
    HELP(6),
    BACKSPACE(8),
    TAB(9),
    NUMPAD_EQUAL(12), // also NUMPAD_5 without NumLock state
    ENTER(13), // also NUMPAD_ENTER
    RESERVED(14),
    SHIFT(16),
    CTRL(17),
    ALT(18),
    PAUSE(19),
    CAPS_LOCK(20),

    KANA(21),
    HANGUL(22),
    EISU(23),
    FINAL(24),
    HANJA(25),
    KANJI(26),

    ESCAPE(27),

    CONVERT(28),
    NONCONVERT(29),
    ACCEPT(30),
    MODECHANGE(31),

    SPACE(32),
    PAGE_UP(33), // also NUMPAD_9 without NumLock state
    PAGE_DOWN(34), // also NUMPAD_3 without NumLock state
    END(35), // also NUMPAD_1 without NumLock state
    HOME(36), // also NUMPAD_7 without NumLock state
    LEFT(37), // also NUMPAD_4 without NumLock state
    UP(38), // also NUMPAD_8 without NumLock state
    RIGHT(39), // also NUMPAD_6 without NumLock state
    DOWN(40), // also NUMPAD_2 without NumLock state

    SELECT(41),
    PRINT(42),
    EXECUTE(43),
    PRINT_SCREEN(44),
    INSERT(45), // also NUMPAD_0 without NumLock state
    DELETE(46), // also NUMPAD_DECIMAL without NumLock state

    DIGIT_0(48),
    DIGIT_1(49),
    DIGIT_2(50),
    DIGIT_3(51),
    DIGIT_4(52),
    DIGIT_5(53),
    DIGIT_6(54),
    DIGIT_7(55),
    DIGIT_8(56),
    DIGIT_9(57),

    COLON(58),
    SEMICOLON(59),
    LESS_THAN(60),
    EQUALS(61),
    GREATER_THAN(62),
    QUESTION_MARK(63),
    AT(64),

    KEY_A(65),
    KEY_B(66),
    KEY_C(67),
    KEY_D(68),
    KEY_E(69),
    KEY_F(70),
    KEY_G(71),
    KEY_H(72),
    KEY_I(73),
    KEY_J(74),
    KEY_K(75),
    KEY_L(76),
    KEY_M(77),
    KEY_N(78),
    KEY_O(79),
    KEY_P(80),
    KEY_Q(81),
    KEY_R(82),
    KEY_S(83),
    KEY_T(84),
    KEY_U(85),
    KEY_V(86),
    KEY_W(87),
    KEY_X(88),
    KEY_Y(89),
    KEY_Z(90),

    OS_LEFT(91),
    OS_RIGHT(92),
    CONTEXT_MENU(93),
    SLEEP(95),

    NUMPAD_0(96),
    NUMPAD_1(97),
    NUMPAD_2(98),
    NUMPAD_3(99),
    NUMPAD_4(100),
    NUMPAD_5(101),
    NUMPAD_6(102),
    NUMPAD_7(103),
    NUMPAD_8(104),
    NUMPAD_9(105),
    NUMPAD_MULTIPLY(106),
    NUMPAD_ADD(107),

    SEPARATOR(108),

    NUMPAD_SUBSTRACT(109),
    NUMPAD_DECIMAL(110),
    NUMPAD_DIVIDE(111),

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
    F13(124),
    F14(125),
    F15(126),
    F16(127),
    F17(128),
    F18(129),
    F19(130),
    F20(131),
    F21(132),
    F22(133),
    F23(134),
    F24(135),

    NUMLOCK(144),
    SCROLL_LOCK(145),

    WIN_OEM_FJ_JISHO(146),
    WIN_OEM_FJ_MASSHOU(147),
    WIN_OEM_FJ_TOUROKU(148),
    WIN_OEM_FJ_LOYA(149),
    WIN_OEM_FJ_ROYA(150),

    CIRCUMFLEX(160),
    EXCLAMATION(161),
    DOUBLE_QUOTE(162),
    HASH(163),
    DOLLAR(164),
    PERCENT(165),
    AMPERSAND(166),
    UNDERSCORE(167),
    OPEN_PAREN(168),
    CLOSE_PAREN(169),
    ASTERISK(170),
    PLUS(171),
    PIPE(172),
    HYPHEN_MINUS(173),
    OPEN_CURLY_BRACKET(174),
    CLOSE_CURLY_BRACKET(175),
    TILDE(176),

    VOLUME_MUTE(181),
    VOLUME_DOWN(182),
    VOLUME_UP(183),

    SEMI_COLON(186),
    EQUAL(187),
    COMMA(188),
    MINUS(189),
    PERIOD(190),
    SLASH(191),
    BACK_QUOTE(192),
    INTL_RO(193),
    NUMPAD_COMMA(194),

    BRACKET_LEFT(219),
    BACKSLASH(220),
    BRACKET_RIGHT(221),
    QUOTE(222),
    META(224),
    ALTGR(225),

    WIN_ICO_HELP(227),
    WIN_ICO_00(228),
    WIN_IME(229),
    WIN_ICO_CLEAR(230),
    WIN_OEM_RESET(233),
    WIN_OEM_JUMP(234),
    WIN_OEM_PA1(235),
    WIN_OEM_PA2(236),
    WIN_OEM_PA3(237),
    WIN_OEM_WSCTRL(238),
    WIN_OEM_CUSEL(239),
    WIN_OEM_ATTN(240),
    WIN_OEM_FINISH(241),
    WIN_OEM_COPY(242),
    WIN_OEM_AUTO(243),
    WIN_OEM_ENLW(244),
    WIN_OEM_BACKTAB(245),
    ATTN(246),
    CRSEL(247),
    EXSEL(248),
    EREOF(249),
    PLAY(250),
    ZOOM(251),
    PA1(253),
    WIN_OEM_CLEAR(254),
    INTL_YEN(255);

    private static Map<Integer, PKeyCodes> codesByKey = new HashMap<>();

    static {
        for (final PKeyCodes code : PKeyCodes.values()) {
            codesByKey.put(code.getCode(), code);
        }
    }

    private final int code;

    private PKeyCodes(final int code) {
        this.code = code;
    }

    public boolean equals(final int code) {
        return this.code == code;
    }

    public int getCode() {
        return this.code;
    }

    public String getCodeToString() {
        return String.valueOf(this.code);
    }

    public static PKeyCodes fromInt(final int code) {
        final PKeyCodes keyCode = codesByKey.get(code);
        return keyCode != null ? keyCode : PKeyCodes.UNKNOWN;
    }

}
