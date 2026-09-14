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
package ubic.gemma.core.util;

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
 * <p>🛑 <b>Generated, not written by hand, and not from one source.</b> The authority is the Unicode
 * Consortium's published <em>Adobe Symbol Encoding to Unicode</em>
 * ({@code unicode.org/Public/MAPPINGS/VENDORS/ADOBE/symbol.txt}), which is Adobe's own mapping and
 * has nothing to do with any operating system. 160 of the 185 entries come straight from it.</p>
 *
 * <p>The remaining 25 need a second source, because <b>Adobe's table maps them into the private-use
 * area itself</b> &mdash; its header says 29 characters carry Corporate Use Subarea assignments,
 * made before Unicode had codepoints for them. Repairing {@code U+F0BE} to {@code U+F8E7} would
 * swap one unrenderable box for another. For those, the glyph's modern assignment is read out of
 * {@code /System/Library/Fonts/Symbol.ttf} by composing its Macintosh cmap (byte &rarr; glyph name)
 * with the reverse of its Unicode cmap (codepoint &rarr; glyph name). Only the table-building step
 * touches that file; nothing at runtime does, and the values below are plain codepoints that behave
 * the same everywhere.</p>
 *
 * <p>Where both sources answer, they agree on 155 of 160 positions. The five that differed were
 * resolved in Adobe's favour &mdash; {@code 0xA2} and {@code 0xB2} are PRIME and DOUBLE PRIME, not
 * the modifier letters the font points at, and {@code 0x27} is CONTAINS AS MEMBER rather than its
 * small variant. None of the five occurs in the corpus, so the backfill migration was unaffected.
 * The fourteen positions uib resolved independently from the Adobe AFM agree with all three.</p>
 *
 * <p>Do not hand-edit an entry below. Correct the rule &mdash; Adobe wins unless Adobe's answer is
 * itself private-use &mdash; and regenerate, or the sources stop agreeing and nothing will say so.</p>
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
        map( 0x20, 0x0020 ); // SPACE
        map( 0x21, 0x0021 ); // EXCLAMATION MARK
        map( 0x22, 0x2200 ); // FOR ALL
        map( 0x23, 0x0023 ); // NUMBER SIGN
        map( 0x24, 0x2203 ); // THERE EXISTS
        map( 0x25, 0x0025 ); // PERCENT SIGN
        map( 0x26, 0x0026 ); // AMPERSAND
        map( 0x27, 0x220B ); // CONTAINS AS MEMBER
        map( 0x28, 0x0028 ); // LEFT PARENTHESIS
        map( 0x29, 0x0029 ); // RIGHT PARENTHESIS
        map( 0x2A, 0x2217 ); // ASTERISK OPERATOR
        map( 0x2B, 0x002B ); // PLUS SIGN
        map( 0x2C, 0x002C ); // COMMA
        map( 0x2D, 0x2212 ); // MINUS SIGN
        map( 0x2E, 0x002E ); // FULL STOP
        map( 0x2F, 0x002F ); // SOLIDUS
        map( 0x30, 0x0030 ); // DIGIT ZERO
        map( 0x31, 0x0031 ); // DIGIT ONE
        map( 0x32, 0x0032 ); // DIGIT TWO
        map( 0x33, 0x0033 ); // DIGIT THREE
        map( 0x34, 0x0034 ); // DIGIT FOUR
        map( 0x35, 0x0035 ); // DIGIT FIVE
        map( 0x36, 0x0036 ); // DIGIT SIX
        map( 0x37, 0x0037 ); // DIGIT SEVEN
        map( 0x38, 0x0038 ); // DIGIT EIGHT
        map( 0x39, 0x0039 ); // DIGIT NINE
        map( 0x3A, 0x003A ); // COLON
        map( 0x3B, 0x003B ); // SEMICOLON
        map( 0x3C, 0x003C ); // LESS-THAN SIGN
        map( 0x3D, 0x003D ); // EQUALS SIGN
        map( 0x3E, 0x003E ); // GREATER-THAN SIGN
        map( 0x3F, 0x003F ); // QUESTION MARK
        map( 0x40, 0x2245 ); // APPROXIMATELY EQUAL TO
        map( 0x41, 0x0391 ); // GREEK CAPITAL LETTER ALPHA
        map( 0x42, 0x0392 ); // GREEK CAPITAL LETTER BETA
        map( 0x43, 0x03A7 ); // GREEK CAPITAL LETTER CHI
        map( 0x44, 0x0394 ); // GREEK CAPITAL LETTER DELTA
        map( 0x45, 0x0395 ); // GREEK CAPITAL LETTER EPSILON
        map( 0x46, 0x03A6 ); // GREEK CAPITAL LETTER PHI
        map( 0x47, 0x0393 ); // GREEK CAPITAL LETTER GAMMA
        map( 0x48, 0x0397 ); // GREEK CAPITAL LETTER ETA
        map( 0x49, 0x0399 ); // GREEK CAPITAL LETTER IOTA
        map( 0x4A, 0x03D1 ); // GREEK THETA SYMBOL
        map( 0x4B, 0x039A ); // GREEK CAPITAL LETTER KAPPA
        map( 0x4C, 0x039B ); // GREEK CAPITAL LETTER LAMDA
        map( 0x4D, 0x039C ); // GREEK CAPITAL LETTER MU
        map( 0x4E, 0x039D ); // GREEK CAPITAL LETTER NU
        map( 0x4F, 0x039F ); // GREEK CAPITAL LETTER OMICRON
        map( 0x50, 0x03A0 ); // GREEK CAPITAL LETTER PI
        map( 0x51, 0x0398 ); // GREEK CAPITAL LETTER THETA
        map( 0x52, 0x03A1 ); // GREEK CAPITAL LETTER RHO
        map( 0x53, 0x03A3 ); // GREEK CAPITAL LETTER SIGMA
        map( 0x54, 0x03A4 ); // GREEK CAPITAL LETTER TAU
        map( 0x55, 0x03A5 ); // GREEK CAPITAL LETTER UPSILON
        map( 0x56, 0x03C2 ); // GREEK SMALL LETTER FINAL SIGMA
        map( 0x57, 0x03A9 ); // GREEK CAPITAL LETTER OMEGA
        map( 0x58, 0x039E ); // GREEK CAPITAL LETTER XI
        map( 0x59, 0x03A8 ); // GREEK CAPITAL LETTER PSI
        map( 0x5A, 0x0396 ); // GREEK CAPITAL LETTER ZETA
        map( 0x5B, 0x005B ); // LEFT SQUARE BRACKET
        map( 0x5C, 0x2234 ); // THEREFORE
        map( 0x5D, 0x005D ); // RIGHT SQUARE BRACKET
        map( 0x5E, 0x22A5 ); // UP TACK
        map( 0x5F, 0x005F ); // LOW LINE
        map( 0x61, 0x03B1 ); // GREEK SMALL LETTER ALPHA
        map( 0x62, 0x03B2 ); // GREEK SMALL LETTER BETA
        map( 0x63, 0x03C7 ); // GREEK SMALL LETTER CHI
        map( 0x64, 0x03B4 ); // GREEK SMALL LETTER DELTA
        map( 0x65, 0x03B5 ); // GREEK SMALL LETTER EPSILON
        map( 0x66, 0x03C6 ); // GREEK SMALL LETTER PHI
        map( 0x67, 0x03B3 ); // GREEK SMALL LETTER GAMMA
        map( 0x68, 0x03B7 ); // GREEK SMALL LETTER ETA
        map( 0x69, 0x03B9 ); // GREEK SMALL LETTER IOTA
        map( 0x6A, 0x03D5 ); // GREEK PHI SYMBOL
        map( 0x6B, 0x03BA ); // GREEK SMALL LETTER KAPPA
        map( 0x6C, 0x03BB ); // GREEK SMALL LETTER LAMDA
        map( 0x6D, 0x03BC ); // GREEK SMALL LETTER MU
        map( 0x6E, 0x03BD ); // GREEK SMALL LETTER NU
        map( 0x6F, 0x03BF ); // GREEK SMALL LETTER OMICRON
        map( 0x70, 0x03C0 ); // GREEK SMALL LETTER PI
        map( 0x71, 0x03B8 ); // GREEK SMALL LETTER THETA
        map( 0x72, 0x03C1 ); // GREEK SMALL LETTER RHO
        map( 0x73, 0x03C3 ); // GREEK SMALL LETTER SIGMA
        map( 0x74, 0x03C4 ); // GREEK SMALL LETTER TAU
        map( 0x75, 0x03C5 ); // GREEK SMALL LETTER UPSILON
        map( 0x76, 0x03D6 ); // GREEK PI SYMBOL
        map( 0x77, 0x03C9 ); // GREEK SMALL LETTER OMEGA
        map( 0x78, 0x03BE ); // GREEK SMALL LETTER XI
        map( 0x79, 0x03C8 ); // GREEK SMALL LETTER PSI
        map( 0x7A, 0x03B6 ); // GREEK SMALL LETTER ZETA
        map( 0x7B, 0x007B ); // LEFT CURLY BRACKET
        map( 0x7C, 0x007C ); // VERTICAL LINE
        map( 0x7D, 0x007D ); // RIGHT CURLY BRACKET
        map( 0x7E, 0x223C ); // TILDE OPERATOR
        map( 0xA0, 0x20AC ); // EURO SIGN
        map( 0xA1, 0x03D2 ); // GREEK UPSILON WITH HOOK SYMBOL
        map( 0xA2, 0x2032 ); // PRIME
        map( 0xA3, 0x2264 ); // LESS-THAN OR EQUAL TO
        map( 0xA4, 0x2044 ); // FRACTION SLASH
        map( 0xA5, 0x221E ); // INFINITY
        map( 0xA6, 0x0192 ); // LATIN SMALL LETTER F WITH HOOK
        map( 0xA7, 0x2663 ); // BLACK CLUB SUIT
        map( 0xA8, 0x2666 ); // BLACK DIAMOND SUIT
        map( 0xA9, 0x2665 ); // BLACK HEART SUIT
        map( 0xAA, 0x2660 ); // BLACK SPADE SUIT
        map( 0xAB, 0x2194 ); // LEFT RIGHT ARROW
        map( 0xAC, 0x2190 ); // LEFTWARDS ARROW
        map( 0xAD, 0x2191 ); // UPWARDS ARROW
        map( 0xAE, 0x2192 ); // RIGHTWARDS ARROW
        map( 0xAF, 0x2193 ); // DOWNWARDS ARROW
        map( 0xB0, 0x00B0 ); // DEGREE SIGN
        map( 0xB1, 0x00B1 ); // PLUS-MINUS SIGN
        map( 0xB2, 0x2033 ); // DOUBLE PRIME
        map( 0xB3, 0x2265 ); // GREATER-THAN OR EQUAL TO
        map( 0xB4, 0x00D7 ); // MULTIPLICATION SIGN
        map( 0xB5, 0x221D ); // PROPORTIONAL TO
        map( 0xB6, 0x2202 ); // PARTIAL DIFFERENTIAL
        map( 0xB7, 0x2022 ); // BULLET
        map( 0xB8, 0x00F7 ); // DIVISION SIGN
        map( 0xB9, 0x2260 ); // NOT EQUAL TO
        map( 0xBA, 0x2261 ); // IDENTICAL TO
        map( 0xBB, 0x2248 ); // ALMOST EQUAL TO
        map( 0xBC, 0x2026 ); // HORIZONTAL ELLIPSIS
        map( 0xBD, 0x23D0 ); // VERTICAL LINE EXTENSION  [font: Adobe assigns private-use here]
        map( 0xBE, 0x23AF ); // HORIZONTAL LINE EXTENSION  [font: Adobe assigns private-use here]
        map( 0xBF, 0x21B5 ); // DOWNWARDS ARROW WITH CORNER LEFTWARDS
        map( 0xC0, 0x2135 ); // ALEF SYMBOL
        map( 0xC1, 0x2111 ); // BLACK-LETTER CAPITAL I
        map( 0xC2, 0x211C ); // BLACK-LETTER CAPITAL R
        map( 0xC3, 0x2118 ); // SCRIPT CAPITAL P
        map( 0xC4, 0x2297 ); // CIRCLED TIMES
        map( 0xC5, 0x2295 ); // CIRCLED PLUS
        map( 0xC6, 0x2205 ); // EMPTY SET
        map( 0xC7, 0x2229 ); // INTERSECTION
        map( 0xC8, 0x222A ); // UNION
        map( 0xC9, 0x2283 ); // SUPERSET OF
        map( 0xCA, 0x2287 ); // SUPERSET OF OR EQUAL TO
        map( 0xCB, 0x2284 ); // NOT A SUBSET OF
        map( 0xCC, 0x2282 ); // SUBSET OF
        map( 0xCD, 0x2286 ); // SUBSET OF OR EQUAL TO
        map( 0xCE, 0x2208 ); // ELEMENT OF
        map( 0xCF, 0x2209 ); // NOT AN ELEMENT OF
        map( 0xD0, 0x2220 ); // ANGLE
        map( 0xD1, 0x2207 ); // NABLA
        map( 0xD5, 0x220F ); // N-ARY PRODUCT
        map( 0xD6, 0x221A ); // SQUARE ROOT
        map( 0xD7, 0x22C5 ); // DOT OPERATOR
        map( 0xD8, 0x00AC ); // NOT SIGN
        map( 0xD9, 0x2227 ); // LOGICAL AND
        map( 0xDA, 0x2228 ); // LOGICAL OR
        map( 0xDB, 0x21D4 ); // LEFT RIGHT DOUBLE ARROW
        map( 0xDC, 0x21D0 ); // LEFTWARDS DOUBLE ARROW
        map( 0xDD, 0x21D1 ); // UPWARDS DOUBLE ARROW
        map( 0xDE, 0x21D2 ); // RIGHTWARDS DOUBLE ARROW
        map( 0xDF, 0x21D3 ); // DOWNWARDS DOUBLE ARROW
        map( 0xE0, 0x25CA ); // LOZENGE
        map( 0xE1, 0x2329 ); // LEFT-POINTING ANGLE BRACKET
        map( 0xE2, 0x00AE ); // REGISTERED SIGN  [font: Adobe assigns private-use here]
        map( 0xE3, 0x00A9 ); // COPYRIGHT SIGN  [font: Adobe assigns private-use here]
        map( 0xE4, 0x2122 ); // TRADE MARK SIGN  [font: Adobe assigns private-use here]
        map( 0xE5, 0x2211 ); // N-ARY SUMMATION
        map( 0xE6, 0x239B ); // LEFT PARENTHESIS UPPER HOOK  [font: Adobe assigns private-use here]
        map( 0xE7, 0x239C ); // LEFT PARENTHESIS EXTENSION  [font: Adobe assigns private-use here]
        map( 0xE8, 0x239D ); // LEFT PARENTHESIS LOWER HOOK  [font: Adobe assigns private-use here]
        map( 0xE9, 0x23A1 ); // LEFT SQUARE BRACKET UPPER CORNER  [font: Adobe assigns private-use here]
        map( 0xEA, 0x23A2 ); // LEFT SQUARE BRACKET EXTENSION  [font: Adobe assigns private-use here]
        map( 0xEB, 0x23A3 ); // LEFT SQUARE BRACKET LOWER CORNER  [font: Adobe assigns private-use here]
        map( 0xEC, 0x23A7 ); // LEFT CURLY BRACKET UPPER HOOK  [font: Adobe assigns private-use here]
        map( 0xED, 0x23A8 ); // LEFT CURLY BRACKET MIDDLE PIECE  [font: Adobe assigns private-use here]
        map( 0xEE, 0x23A9 ); // LEFT CURLY BRACKET LOWER HOOK  [font: Adobe assigns private-use here]
        map( 0xEF, 0x23AA ); // CURLY BRACKET EXTENSION  [font: Adobe assigns private-use here]
        map( 0xF1, 0x232A ); // RIGHT-POINTING ANGLE BRACKET
        map( 0xF2, 0x222B ); // INTEGRAL
        map( 0xF3, 0x2320 ); // TOP HALF INTEGRAL
        map( 0xF4, 0x23AE ); // INTEGRAL EXTENSION  [font: Adobe assigns private-use here]
        map( 0xF5, 0x2321 ); // BOTTOM HALF INTEGRAL
        map( 0xF6, 0x239E ); // RIGHT PARENTHESIS UPPER HOOK  [font: Adobe assigns private-use here]
        map( 0xF7, 0x239F ); // RIGHT PARENTHESIS EXTENSION  [font: Adobe assigns private-use here]
        map( 0xF8, 0x23A0 ); // RIGHT PARENTHESIS LOWER HOOK  [font: Adobe assigns private-use here]
        map( 0xF9, 0x23A4 ); // RIGHT SQUARE BRACKET UPPER CORNER  [font: Adobe assigns private-use here]
        map( 0xFA, 0x23A5 ); // RIGHT SQUARE BRACKET EXTENSION  [font: Adobe assigns private-use here]
        map( 0xFB, 0x23A6 ); // RIGHT SQUARE BRACKET LOWER CORNER  [font: Adobe assigns private-use here]
        map( 0xFC, 0x23AB ); // RIGHT CURLY BRACKET UPPER HOOK  [font: Adobe assigns private-use here]
        map( 0xFD, 0x23AC ); // RIGHT CURLY BRACKET MIDDLE PIECE  [font: Adobe assigns private-use here]
        map( 0xFE, 0x23AD ); // RIGHT CURLY BRACKET LOWER HOOK  [font: Adobe assigns private-use here]
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
