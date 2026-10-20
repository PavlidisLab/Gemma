/*
 * The baseCode project
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * String helpers (R-style name munging, common prefix/suffix, CSV split, etc.).
 * <p>
 * Ported in-tree from {@code ubic.basecode.util.StringUtil} as part of the Phase 3
 * baseCode util retirement (see {@code BASECODE_DEP_AUDIT.md}). Methods not called
 * by Gemma have been dropped: {@code containsValidCharacter}, {@code cvs2tsv},
 * {@code isLatinLetter}, {@code makeValidForR}, {@code twoStringHashKey}. The
 * single-arg legacy {@code makeValidForR(String)} is gone; callers use
 * {@link #makeNames(String)} directly.
 * <p>
 * Distinct from {@link StringUtils} (plural) which extends
 * {@link org.apache.commons.lang3.StringUtils}.
 *
 * @author pavlidis
 */
public class StringUtil {

    /**
     * @param appendee  The string to be added to
     * @param appendant The string to add to the end of the appendee
     * @param separator The string to put between the joined strings, if necessary.
     * @return appendee + separator + appendant unless appendee is empty, in which case the appendant is returned.
     */
    public static String append( @Nullable String appendee, String appendant, String separator ) {
        if ( org.apache.commons.lang3.StringUtils.isBlank( appendee ) ) {
            return appendant;
        }
        return appendee + separator + appendant;
    }

    /**
     * Given a set of strings, identify any prefix they have in common.
     *
     * @return the common prefix, null if there isn't one.
     */
    @Nullable
    public static String commonPrefix( Collection<String> strings ) {
        // find the shortest string; this is the maximum length of the prefix. It is itself the prefix to look for.
        String shortest = shortestString( strings );

        if ( shortest == null || shortest.length() == 0 ) return null;

        String test = shortest;
        while ( test.length() > 0 ) {
            boolean found = true;
            for ( String string : strings ) {
                if ( !string.startsWith( test ) ) {
                    found = false;
                    break;
                }
            }
            if ( found ) return test;
            test = test.substring( 0, test.length() - 1 );
        }
        return null;
    }

    /**
     * Given a set of strings, identify any suffix they have in common.
     *
     * @return the common suffix, null if there isn't one.
     */
    @Nullable
    public static String commonSuffix( Collection<String> strings ) {
        String shortest = shortestString( strings );

        if ( shortest == null || shortest.length() == 0 ) return null;

        String test = shortest;
        while ( test.length() > 0 ) {
            boolean found = true;
            for ( String string : strings ) {
                if ( !string.endsWith( test ) ) {
                    found = false;
                    break;
                }
            }
            if ( found ) return test;
            test = test.substring( 1 );
        }
        return null;
    }

    /**
     * Split a single CSV-formatted line into fields.
     */
    public static String[] csvSplit( String line ) {
        try ( CSVParser parser = CSVParser.parse( new StringReader( line ), CSVFormat.DEFAULT ) ) {
            for ( CSVRecord record : parser ) {
                return record.values();
            }
            throw new IllegalArgumentException( "No CSV records found in line." );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Mimics the {@code make.names} method in R.
     * @param strings a list of strings to be made valid for R
     * @param unique  if true, will ensure that the names are unique by appending a number to duplicates as per
     * {@link #makeUnique(String[])}
     * @author poirigui
     */
    public static String[] makeNames( String[] strings, boolean unique ) {
        String[] result = new String[strings.length];
        if ( unique ) {
            Map<String, Integer> counts = new HashMap<>();
            for ( int i = 0; i < strings.length; i++ ) {
                String s = strings[i];
                String rs = makeNames( s );
                if ( counts.containsKey( rs ) ) {
                    int count = counts.get( rs );
                    result[i] = rs + "." + count;
                    counts.put( rs, count + 1 );
                } else {
                    result[i] = rs;
                    counts.put( rs, 1 );
                }
            }
        } else {
            for ( int i = 0; i < strings.length; i++ ) {
                result[i] = makeNames( strings[i] );
            }
        }
        return result;
    }

    private static final String[] R_RESERVED_WORDS = {
        "if", "else", "repeat", "while", "function", "for", "in", "next", "break",
        "TRUE", "FALSE", "NULL", "Inf", "NaN", "NA", "NA_integer_", "NA_real_", "NA_character_", "NA_complex_",
    };

    /**
     * Mimics the {@code make.names} method in R for a single string.
     * @author paul
     */
    public static String makeNames( @Nullable String s ) {
        if ( s == null ) {
            return "NA";
        }
        if ( s.isEmpty()
            // starts with a non-letter or non-dot
            || ( !Character.isAlphabetic( s.charAt( 0 ) ) && s.charAt( 0 ) != '.' )
            // dot followed by a digit
            || ( s.charAt( 0 ) == '.' && s.length() > 1 && Character.isDigit( s.charAt( 1 ) ) ) ) {
            return "X" + s.replaceAll( "[^A-Za-z0-9._]", "." );
        }
        if ( org.apache.commons.lang3.StringUtils.equalsAny( s, R_RESERVED_WORDS ) ) {
            return s + ".";
        }
        return s.replaceAll( "[^A-Za-z0-9._]", "." );
    }

    /**
     * Mimics the {@code make.unique} method in R.
     * <p>
     * Duplicated values in the input array will be suffixed with a dot and a number, starting from 1.
     * @author poirigui
     */
    public static String[] makeUnique( String[] strings ) {
        Map<String, Integer> counts = new HashMap<>();
        String[] result = new String[strings.length];
        for ( int i = 0; i < strings.length; i++ ) {
            String cn = strings[i];
            if ( counts.containsKey( cn ) ) {
                int count = counts.get( cn );
                result[i] = cn + "." + count;
                counts.put( cn, count + 1 );
            } else {
                result[i] = cn;
                counts.put( cn, 1 );
            }
        }
        return result;
    }

    @Nullable
    private static String shortestString( Collection<String> strings ) {
        String shortest = null;
        for ( String string : strings ) {
            if ( shortest == null || string.length() < shortest.length() ) shortest = string;
        }
        return shortest;
    }
}
