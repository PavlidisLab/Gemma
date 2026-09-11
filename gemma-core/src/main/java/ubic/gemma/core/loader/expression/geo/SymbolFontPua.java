/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.loader.expression.geo;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Repairs Adobe <b>Symbol</b>-font text that was mapped into the Unicode Private Use Area.
 *
 * <h2>What the damage is</h2>
 *
 * <p>A submitter types {@code β} in Word as a Symbol-font {@code b}. Symbol is a font-specific
 * encoding, not a character set, so the byte {@code 0x62} means "whatever Symbol draws at 0x62".
 * Software that maps such text into Unicode naively adds {@code 0xF000} to each byte, landing in the
 * Private Use Area at {@code U+F062} &mdash; a region where meaning is font-specific <em>by
 * definition</em>, so the codepoint has no name and no font can render it. It reaches a reader as a
 * tofu box. GSE245831 stored {@code "A" + U+F062 + " burden"} and the browser showed
 * {@code "A▢ burden"} for what should read {@code "Aβ burden"} (uib, 2026-09-10).</p>
 *
 * <p>The transform is mechanical and reversible: {@code U+F0xx} is whatever Adobe Symbol holds at
 * position {@code 0xxx}.</p>
 *
 * <h2>Where the table comes from</h2>
 *
 * <p>🛑 <b>Generated from the font, not written by hand.</b> The macOS system
 * {@code /System/Library/Fonts/Symbol.ttf} carries both cmap subtables needed: the Macintosh
 * (platform 1) one maps byte &rarr; glyph name, and the Unicode (platform 0) one maps codepoint
 * &rarr; glyph name. Composing the first with the reverse of the second yields byte &rarr; proper
 * Unicode. 185 of the 190 encoded positions resolve to a non-PUA codepoint that way.</p>
 *
 * <p>It was cross-checked against the fourteen positions uib resolved independently from the Adobe
 * AFM while scanning the corpus; all fourteen agree. Do not hand-edit an entry below &mdash;
 * correct the generator and regenerate, or the two sources stop agreeing and nothing will say so.</p>
 *
 * <h2>What is deliberately NOT mapped</h2>
 *
 * <p>Five positions have no standard Unicode equivalent even in the font, and are left exactly as
 * they are rather than guessed at: {@code radicalex} (0x60, a radical-sign extender),
 * {@code registerserif} / {@code copyrightserif} / {@code trademarkserif} (0xD2&ndash;0xD4, serif
 * variants of &reg; &copy; &trade;) and {@code apple} (0xF0). None occurs in the corpus. A
 * codepoint this class cannot map is reported by {@link #unmappable(String)} so it can be looked at
 * in context, which is the only way to settle it.</p>
 *
 * <p>{@code U+FFFD} is out of scope entirely: it is a replacement character, meaning the original
 * byte is already gone and cannot be recovered from the stored text.</p>
 *
 * @author gembro
 */
@Slf4j
public class SymbolFontPua {

    /** Start of the block Symbol-encoded text lands in: {@code 0xF000 + 0x20}. */
    private static final char FIRST = 0xF020;
    /** Last encoded Symbol position, {@code 0xF000 + 0xFE}. */
    private static final char LAST = 0xF0FE;

    /**
     * Indexed by {@code codepoint - FIRST}; {@code 0} means "no mapping, leave it alone".
     */
    private static final char[] REPLACEMENT = new char[LAST - FIRST + 1];

    private static void map( int symbolPosition, int unicode ) {
        REPLACEMENT[( 0xF000 + symbolPosition ) - FIRST] = ( char ) unicode;
    }

    static {
        map( 0x20, 0x0020 ); // space -> SPACE
        map( 0x21, 0x0021 ); // exclam -> EXCLAMATION MARK
        map( 0x22, 0x2200 ); // universal -> FOR ALL
        map( 0x23, 0x0023 ); // numbersign -> NUMBER SIGN
        map( 0x24, 0x2203 ); // existential -> THERE EXISTS
        map( 0x25, 0x0025 ); // percent -> PERCENT SIGN
        map( 0x26, 0x0026 ); // ampersand -> AMPERSAND
        map( 0x27, 0x220D ); // suchthat -> SMALL CONTAINS AS MEMBER
        map( 0x28, 0x0028 ); // parenleft -> LEFT PARENTHESIS
        map( 0x29, 0x0029 ); // parenright -> RIGHT PARENTHESIS
        map( 0x2A, 0x2217 ); // asteriskmath -> ASTERISK OPERATOR
        map( 0x2B, 0x002B ); // plus -> PLUS SIGN
        map( 0x2C, 0x002C ); // comma -> COMMA
        map( 0x2D, 0x2212 ); // minus -> MINUS SIGN
        map( 0x2E, 0x002E ); // period -> FULL STOP
        map( 0x2F, 0x002F ); // slash -> SOLIDUS
        map( 0x30, 0x0030 ); // zero -> DIGIT ZERO
        map( 0x31, 0x0031 ); // one -> DIGIT ONE
        map( 0x32, 0x0032 ); // two -> DIGIT TWO
        map( 0x33, 0x0033 ); // three -> DIGIT THREE
        map( 0x34, 0x0034 ); // four -> DIGIT FOUR
        map( 0x35, 0x0035 ); // five -> DIGIT FIVE
        map( 0x36, 0x0036 ); // six -> DIGIT SIX
        map( 0x37, 0x0037 ); // seven -> DIGIT SEVEN
        map( 0x38, 0x0038 ); // eight -> DIGIT EIGHT
        map( 0x39, 0x0039 ); // nine -> DIGIT NINE
        map( 0x3A, 0x003A ); // colon -> COLON
        map( 0x3B, 0x003B ); // semicolon -> SEMICOLON
        map( 0x3C, 0x003C ); // less -> LESS-THAN SIGN
        map( 0x3D, 0x003D ); // equal -> EQUALS SIGN
        map( 0x3E, 0x003E ); // greater -> GREATER-THAN SIGN
        map( 0x3F, 0x003F ); // question -> QUESTION MARK
        map( 0x40, 0x2245 ); // congruent -> APPROXIMATELY EQUAL TO
        map( 0x41, 0x0391 ); // Alpha -> GREEK CAPITAL LETTER ALPHA
        map( 0x42, 0x0392 ); // Beta -> GREEK CAPITAL LETTER BETA
        map( 0x43, 0x03A7 ); // Chi -> GREEK CAPITAL LETTER CHI
        map( 0x44, 0x0394 ); // Delta -> GREEK CAPITAL LETTER DELTA
        map( 0x45, 0x0395 ); // Epsilon -> GREEK CAPITAL LETTER EPSILON
        map( 0x46, 0x03A6 ); // Phi -> GREEK CAPITAL LETTER PHI
        map( 0x47, 0x0393 ); // Gamma -> GREEK CAPITAL LETTER GAMMA
        map( 0x48, 0x0397 ); // Eta -> GREEK CAPITAL LETTER ETA
        map( 0x49, 0x0399 ); // Iota -> GREEK CAPITAL LETTER IOTA
        map( 0x4A, 0x03D1 ); // theta1 -> GREEK THETA SYMBOL
        map( 0x4B, 0x039A ); // Kappa -> GREEK CAPITAL LETTER KAPPA
        map( 0x4C, 0x039B ); // Lambda -> GREEK CAPITAL LETTER LAMDA
        map( 0x4D, 0x039C ); // Mu -> GREEK CAPITAL LETTER MU
        map( 0x4E, 0x039D ); // Nu -> GREEK CAPITAL LETTER NU
        map( 0x4F, 0x039F ); // Omicron -> GREEK CAPITAL LETTER OMICRON
        map( 0x50, 0x03A0 ); // Pi -> GREEK CAPITAL LETTER PI
        map( 0x51, 0x0398 ); // Theta -> GREEK CAPITAL LETTER THETA
        map( 0x52, 0x03A1 ); // Rho -> GREEK CAPITAL LETTER RHO
        map( 0x53, 0x03A3 ); // Sigma -> GREEK CAPITAL LETTER SIGMA
        map( 0x54, 0x03A4 ); // Tau -> GREEK CAPITAL LETTER TAU
        map( 0x55, 0x03A5 ); // Upsilon -> GREEK CAPITAL LETTER UPSILON
        map( 0x56, 0x03C2 ); // sigma1 -> GREEK SMALL LETTER FINAL SIGMA
        map( 0x57, 0x03A9 ); // Omega -> GREEK CAPITAL LETTER OMEGA
        map( 0x58, 0x039E ); // Xi -> GREEK CAPITAL LETTER XI
        map( 0x59, 0x03A8 ); // Psi -> GREEK CAPITAL LETTER PSI
        map( 0x5A, 0x0396 ); // Zeta -> GREEK CAPITAL LETTER ZETA
        map( 0x5B, 0x005B ); // bracketleft -> LEFT SQUARE BRACKET
        map( 0x5C, 0x2234 ); // therefore -> THEREFORE
        map( 0x5D, 0x005D ); // bracketright -> RIGHT SQUARE BRACKET
        map( 0x5E, 0x22A5 ); // perpendicular -> UP TACK
        map( 0x5F, 0x005F ); // underscore -> LOW LINE
        map( 0x61, 0x03B1 ); // alpha -> GREEK SMALL LETTER ALPHA
        map( 0x62, 0x03B2 ); // beta -> GREEK SMALL LETTER BETA
        map( 0x63, 0x03C7 ); // chi -> GREEK SMALL LETTER CHI
        map( 0x64, 0x03B4 ); // delta -> GREEK SMALL LETTER DELTA
        map( 0x65, 0x03B5 ); // epsilon -> GREEK SMALL LETTER EPSILON
        map( 0x66, 0x03C6 ); // phi -> GREEK SMALL LETTER PHI
        map( 0x67, 0x03B3 ); // gamma -> GREEK SMALL LETTER GAMMA
        map( 0x68, 0x03B7 ); // eta -> GREEK SMALL LETTER ETA
        map( 0x69, 0x03B9 ); // iota -> GREEK SMALL LETTER IOTA
        map( 0x6A, 0x03D5 ); // phi1 -> GREEK PHI SYMBOL
        map( 0x6B, 0x03BA ); // kappa -> GREEK SMALL LETTER KAPPA
        map( 0x6C, 0x03BB ); // lambda -> GREEK SMALL LETTER LAMDA
        map( 0x6D, 0x03BC ); // mu -> GREEK SMALL LETTER MU
        map( 0x6E, 0x03BD ); // nu -> GREEK SMALL LETTER NU
        map( 0x6F, 0x03BF ); // omicron -> GREEK SMALL LETTER OMICRON
        map( 0x70, 0x03C0 ); // pi -> GREEK SMALL LETTER PI
        map( 0x71, 0x03B8 ); // theta -> GREEK SMALL LETTER THETA
        map( 0x72, 0x03C1 ); // rho -> GREEK SMALL LETTER RHO
        map( 0x73, 0x03C3 ); // sigma -> GREEK SMALL LETTER SIGMA
        map( 0x74, 0x03C4 ); // tau -> GREEK SMALL LETTER TAU
        map( 0x75, 0x03C5 ); // upsilon -> GREEK SMALL LETTER UPSILON
        map( 0x76, 0x03D6 ); // omega1 -> GREEK PI SYMBOL
        map( 0x77, 0x03C9 ); // omega -> GREEK SMALL LETTER OMEGA
        map( 0x78, 0x03BE ); // xi -> GREEK SMALL LETTER XI
        map( 0x79, 0x03C8 ); // psi -> GREEK SMALL LETTER PSI
        map( 0x7A, 0x03B6 ); // zeta -> GREEK SMALL LETTER ZETA
        map( 0x7B, 0x007B ); // braceleft -> LEFT CURLY BRACKET
        map( 0x7C, 0x007C ); // bar -> VERTICAL LINE
        map( 0x7D, 0x007D ); // braceright -> RIGHT CURLY BRACKET
        map( 0x7E, 0x223C ); // similar -> TILDE OPERATOR
        map( 0xA0, 0x20AC ); // Euro -> EURO SIGN
        map( 0xA1, 0x03D2 ); // Upsilon1 -> GREEK UPSILON WITH HOOK SYMBOL
        map( 0xA2, 0x02B9 ); // minute -> MODIFIER LETTER PRIME
        map( 0xA3, 0x2264 ); // lessequal -> LESS-THAN OR EQUAL TO
        map( 0xA4, 0x2044 ); // fraction -> FRACTION SLASH
        map( 0xA5, 0x221E ); // infinity -> INFINITY
        map( 0xA6, 0x0192 ); // florin -> LATIN SMALL LETTER F WITH HOOK
        map( 0xA7, 0x2663 ); // club -> BLACK CLUB SUIT
        map( 0xA8, 0x2666 ); // diamond -> BLACK DIAMOND SUIT
        map( 0xA9, 0x2665 ); // heart -> BLACK HEART SUIT
        map( 0xAA, 0x2660 ); // spade -> BLACK SPADE SUIT
        map( 0xAB, 0x2194 ); // arrowboth -> LEFT RIGHT ARROW
        map( 0xAC, 0x2190 ); // arrowleft -> LEFTWARDS ARROW
        map( 0xAD, 0x2191 ); // arrowup -> UPWARDS ARROW
        map( 0xAE, 0x2192 ); // arrowright -> RIGHTWARDS ARROW
        map( 0xAF, 0x2193 ); // arrowdown -> DOWNWARDS ARROW
        map( 0xB0, 0x00B0 ); // degree -> DEGREE SIGN
        map( 0xB1, 0x00B1 ); // plusminus -> PLUS-MINUS SIGN
        map( 0xB2, 0x02BA ); // second -> MODIFIER LETTER DOUBLE PRIME
        map( 0xB3, 0x2265 ); // greaterequal -> GREATER-THAN OR EQUAL TO
        map( 0xB4, 0x00D7 ); // multiply -> MULTIPLICATION SIGN
        map( 0xB5, 0x221D ); // proportional -> PROPORTIONAL TO
        map( 0xB6, 0x2202 ); // partialdiff -> PARTIAL DIFFERENTIAL
        map( 0xB7, 0x2022 ); // bullet -> BULLET
        map( 0xB8, 0x00F7 ); // divide -> DIVISION SIGN
        map( 0xB9, 0x2260 ); // notequal -> NOT EQUAL TO
        map( 0xBA, 0x2261 ); // equivalence -> IDENTICAL TO
        map( 0xBB, 0x2248 ); // approxequal -> ALMOST EQUAL TO
        map( 0xBC, 0x2026 ); // ellipsis -> HORIZONTAL ELLIPSIS
        map( 0xBD, 0x23D0 ); // arrowvertex -> VERTICAL LINE EXTENSION
        map( 0xBE, 0x23AF ); // arrowhorizex -> HORIZONTAL LINE EXTENSION
        map( 0xBF, 0x21B5 ); // carriagereturn -> DOWNWARDS ARROW WITH CORNER LEFTWARDS
        map( 0xC0, 0x2135 ); // aleph -> ALEF SYMBOL
        map( 0xC1, 0x2111 ); // Ifraktur -> BLACK-LETTER CAPITAL I
        map( 0xC2, 0x211C ); // Rfraktur -> BLACK-LETTER CAPITAL R
        map( 0xC3, 0x2118 ); // weierstrass -> SCRIPT CAPITAL P
        map( 0xC4, 0x2297 ); // circlemultiply -> CIRCLED TIMES
        map( 0xC5, 0x2295 ); // circleplus -> CIRCLED PLUS
        map( 0xC6, 0x2205 ); // emptyset -> EMPTY SET
        map( 0xC7, 0x2229 ); // intersection -> INTERSECTION
        map( 0xC8, 0x222A ); // union -> UNION
        map( 0xC9, 0x2283 ); // propersuperset -> SUPERSET OF
        map( 0xCA, 0x2287 ); // reflexsuperset -> SUPERSET OF OR EQUAL TO
        map( 0xCB, 0x2284 ); // notsubset -> NOT A SUBSET OF
        map( 0xCC, 0x2282 ); // propersubset -> SUBSET OF
        map( 0xCD, 0x2286 ); // reflexsubset -> SUBSET OF OR EQUAL TO
        map( 0xCE, 0x2208 ); // element -> ELEMENT OF
        map( 0xCF, 0x2209 ); // notelement -> NOT AN ELEMENT OF
        map( 0xD0, 0x2220 ); // angle -> ANGLE
        map( 0xD1, 0x2207 ); // gradient -> NABLA
        map( 0xD5, 0x220F ); // product -> N-ARY PRODUCT
        map( 0xD6, 0x221A ); // radical -> SQUARE ROOT
        map( 0xD7, 0x22C5 ); // dotmath -> DOT OPERATOR
        map( 0xD8, 0x00AC ); // logicalnot -> NOT SIGN
        map( 0xD9, 0x2227 ); // logicaland -> LOGICAL AND
        map( 0xDA, 0x2228 ); // logicalor -> LOGICAL OR
        map( 0xDB, 0x21D4 ); // arrowdblboth -> LEFT RIGHT DOUBLE ARROW
        map( 0xDC, 0x21D0 ); // arrowdblleft -> LEFTWARDS DOUBLE ARROW
        map( 0xDD, 0x21D1 ); // arrowdblup -> UPWARDS DOUBLE ARROW
        map( 0xDE, 0x21D2 ); // arrowdblright -> RIGHTWARDS DOUBLE ARROW
        map( 0xDF, 0x21D3 ); // arrowdbldown -> DOWNWARDS DOUBLE ARROW
        map( 0xE0, 0x25CA ); // lozenge -> LOZENGE
        map( 0xE1, 0x3008 ); // angleleft -> LEFT ANGLE BRACKET
        map( 0xE2, 0x00AE ); // registersans -> REGISTERED SIGN
        map( 0xE3, 0x00A9 ); // copyrightsans -> COPYRIGHT SIGN
        map( 0xE4, 0x2122 ); // trademarksans -> TRADE MARK SIGN
        map( 0xE5, 0x2211 ); // summation -> N-ARY SUMMATION
        map( 0xE6, 0x239B ); // parenlefttp -> LEFT PARENTHESIS UPPER HOOK
        map( 0xE7, 0x239C ); // parenleftex -> LEFT PARENTHESIS EXTENSION
        map( 0xE8, 0x239D ); // parenleftbt -> LEFT PARENTHESIS LOWER HOOK
        map( 0xE9, 0x23A1 ); // bracketlefttp -> LEFT SQUARE BRACKET UPPER CORNER
        map( 0xEA, 0x23A2 ); // bracketleftex -> LEFT SQUARE BRACKET EXTENSION
        map( 0xEB, 0x23A3 ); // bracketleftbt -> LEFT SQUARE BRACKET LOWER CORNER
        map( 0xEC, 0x23A7 ); // bracelefttp -> LEFT CURLY BRACKET UPPER HOOK
        map( 0xED, 0x23A8 ); // braceleftmid -> LEFT CURLY BRACKET MIDDLE PIECE
        map( 0xEE, 0x23A9 ); // braceleftbt -> LEFT CURLY BRACKET LOWER HOOK
        map( 0xEF, 0x23AA ); // braceex -> CURLY BRACKET EXTENSION
        map( 0xF1, 0x3009 ); // angleright -> RIGHT ANGLE BRACKET
        map( 0xF2, 0x222B ); // integral -> INTEGRAL
        map( 0xF3, 0x2320 ); // integraltp -> TOP HALF INTEGRAL
        map( 0xF4, 0x23AE ); // integralex -> INTEGRAL EXTENSION
        map( 0xF5, 0x2321 ); // integralbt -> BOTTOM HALF INTEGRAL
        map( 0xF6, 0x239E ); // parenrighttp -> RIGHT PARENTHESIS UPPER HOOK
        map( 0xF7, 0x239F ); // parenrightex -> RIGHT PARENTHESIS EXTENSION
        map( 0xF8, 0x23A0 ); // parenrightbt -> RIGHT PARENTHESIS LOWER HOOK
        map( 0xF9, 0x23A4 ); // bracketrighttp -> RIGHT SQUARE BRACKET UPPER CORNER
        map( 0xFA, 0x23A5 ); // bracketrightex -> RIGHT SQUARE BRACKET EXTENSION
        map( 0xFB, 0x23A6 ); // bracketrightbt -> RIGHT SQUARE BRACKET LOWER CORNER
        map( 0xFC, 0x23AB ); // bracerighttp -> RIGHT CURLY BRACKET UPPER HOOK
        map( 0xFD, 0x23AC ); // bracerightmid -> RIGHT CURLY BRACKET MIDDLE PIECE
        map( 0xFE, 0x23AD ); // bracerightbt -> RIGHT CURLY BRACKET LOWER HOOK
    }

    /**
     * Whether the text holds any codepoint this class would change.
     * <p>
     * Cheap on purpose: every parsed GEO metadata line is offered to {@link #repair(String)}, and
     * almost none of them contain one of these.
     */
    public static boolean isAffected( @Nullable String s ) {
        if ( s == null ) {
            return false;
        }
        for ( int i = 0; i < s.length(); i++ ) {
            char c = s.charAt( i );
            if ( c >= FIRST && c <= LAST && REPLACEMENT[c - FIRST] != 0 ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Replace every mapped Symbol-font PUA codepoint with the character it was meant to be.
     * <p>
     * Returns the argument unchanged &mdash; the same instance &mdash; when there is nothing to do,
     * which is the overwhelmingly common case.
     */
    @Nullable
    public static String repair( @Nullable String s ) {
        if ( !isAffected( s ) ) {
            return s;
        }
        StringBuilder b = new StringBuilder( s.length() );
        for ( int i = 0; i < s.length(); i++ ) {
            char c = s.charAt( i );
            char r = ( c >= FIRST && c <= LAST ) ? REPLACEMENT[c - FIRST] : 0;
            b.append( r != 0 ? r : c );
        }
        return b.toString();
    }

    /**
     * The private-use codepoints in {@code s} that {@link #repair(String)} would leave in place.
     * <p>
     * Reported rather than guessed at. A caller that logs these is naming exactly the characters a
     * human has to resolve from context; an empty set means the text is either clean or fully
     * repairable.
     *
     * @return the offending codepoints, ascending, or an empty set
     */
    public static Set<Integer> unmappable( @Nullable String s ) {
        if ( s == null ) {
            return Collections.emptySet();
        }
        Set<Integer> out = null;
        for ( int i = 0; i < s.length(); i++ ) {
            char c = s.charAt( i );
            boolean privateUse = c >= 0xE000 && c <= 0xF8FF;
            if ( privateUse && !( c >= FIRST && c <= LAST && REPLACEMENT[c - FIRST] != 0 ) ) {
                if ( out == null ) {
                    out = new TreeSet<>();
                }
                out.add( ( int ) c );
            }
        }
        return out != null ? out : Collections.emptySet();
    }

    private SymbolFontPua() {
    }
}
