/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.core.analysis.sequence;

/**
 * Used to assign a bin to a chromosome location, identify bins for a range, or to generate SQL to add to a query on a
 * GoldenPath database.
 * Directly ported from Jim Kent's binRange.c and hdb.c.
 *
 * @author pavlidis
 */
public class SequenceBinUtils {

    /*
     * Basic idea: bin is identified by right-shifting the start and end values until they are equal. The number of
     * shifts gives an offset.
     */

    private static final int _binFirstShift = 17; /* How much to shift to get to finest bin. */
    private static final int _binNextShift = 3; /* How much to shift to get to next larger bin. */

    private static final int[] binOffsetsExtended = { 4096 + 512 + 64 + 8 + 1, 512 + 64 + 8 + 1, 64 + 8 + 1, 8 + 1, 1,
            0 };

    private static final int[] binOffsets = { 512 + 64 + 8 + 1, 64 + 8 + 1, 8 + 1, 1, 0 };
    private static final int BINRANGE_MAXEND_512M = ( 512 * 1024 * 1024 );
    private static final int _binOffsetOldToExtended = 4681;

    /**
     * Directly ported from jksrc binRange.c and hdb.c
     * From the binRange.c comments: There's a bin for each 128k segment, for each 1M segment, for each 8M segment, for
     * each 64M segment, and for each chromosome (which is assumed to be less than 512M.) A range goes into the smallest
     * bin it will fit in.
     *
     * @param table The alias of the table (SQL) or class (HQL)
     * @param start start
     * @param end   end
     * @return clause that will restrict to relevant bins to query. This should be ANDed into your WHERE clause.
     */
    public static String addBinToQuery( String table, Long start, Long end ) {
        if ( end <= SequenceBinUtils.BINRANGE_MAXEND_512M ) {
            return SequenceBinUtils.hAddBinToQueryStandard( table, start, end, Boolean.TRUE );
        }
        return SequenceBinUtils.hAddBinToQueryExtended( table, start, end );
    }

    /**
     * return bin that this start-end segment is in
     *
     * @param start start
     * @param end   end
     * @return bin
     */
    public static int binFromRange( int start, int end ) {
        if ( end <= SequenceBinUtils.BINRANGE_MAXEND_512M )
            return SequenceBinUtils.binFromRangeStandard( start, end );

        return SequenceBinUtils.binFromRangeExtended( start, end );
    }

    /**
     * Given start,end in chromosome coordinates assign it a bin. There's a bin for each 128k segment, for each 1M
     * segment, for each 8M segment, for each 64M segment, for each 512M segment, and one top level bin for 4Gb. Note,
     * since start and end are int's, the practical limit is up to 2Gb-1, and thus, only four result bins on the second
     * level. A range goes into the smallest bin it will fit in.
     *
     * @param start start
     * @param end   end
     * @return bin
     */
    private static int binFromRangeExtended( int start, int end ) {
        return SequenceBinUtils.binFromRangeParam( start, end, SequenceBinUtils.binOffsetsExtended,
                SequenceBinUtils._binOffsetOldToExtended );
    }

    /**
     * Given start,end in chromosome coordinates assign it a bin. There's a bin for each 128k segment, for each 1M
     * segment, for each 8M segment, for each 64M segment, and for each chromosome (which is assumed to be less than
     * 512M.) A range goes into the smallest bin it will fit in.
     *
     * @param start start
     * @param end   end
     * @return bin
     */
    private static int binFromRangeStandard( int start, int end ) {
        return SequenceBinUtils.binFromRangeParam( start, end, SequenceBinUtils.binOffsets, 0 );
    }

    /**
     * Return offset for bins of a given level.
     */
    private static int binOffset( int level ) {
        assert ( level >= 0 && level < SequenceBinUtils.binOffsets.length );
        return SequenceBinUtils.binOffsets[level];
    }

    /**
     * Return offset for bins of a given level.
     */
    private static int binOffsetExtended( int level ) {
        assert ( level >= 0 && level < SequenceBinUtils.binOffsetsExtended.length );
        return SequenceBinUtils.binOffsetsExtended[level] + SequenceBinUtils._binOffsetOldToExtended;
    }

    /**
     * @param startBin start
     * @param endBin   end
     * @param column   column
     * @param query    query
     * @param offset   offset
     * @return If the start and end are in the same bin, we return an 'equals' clause; otherwise a range is returned.
     */
    private static String getClause( long startBin, long endBin, String column, String query, int offset ) {
        if ( startBin == endBin ) {
            query = query + column + " = " + ( startBin + offset );
        } else {
            query = query + column + " >= " + ( startBin + offset ) + " and " + column + " <= " + ( endBin + offset );
        }
        return query;
    }

    /**
     * Add clause that will restrict to relevant bins to query.
     */
    private static String hAddBinToQueryExtended( String table, long start, long end ) {
        int bFirstShift = SequenceBinUtils._binFirstShift, bNextShift = SequenceBinUtils._binNextShift;
        long startBin = ( start >> bFirstShift ), endBin = ( ( end - 1 ) >> bFirstShift );
        int i, levels = SequenceBinUtils.binOffsetsExtended.length;
        String column = table + ".bin";
        String query = " (";

        if ( start < SequenceBinUtils.BINRANGE_MAXEND_512M ) {
            query = query + SequenceBinUtils
                    .hAddBinToQueryStandard( table, start, SequenceBinUtils.BINRANGE_MAXEND_512M, false );
            query = query + " or ";
        }

        for ( i = 0; i < levels; ++i ) {
            int offset = SequenceBinUtils.binOffsetExtended( i );
            if ( i != 0 ) {
                query = query + " or ";
            }
            query = SequenceBinUtils.getClause( startBin, endBin, column, query, offset );
            startBin >>= bNextShift;
            endBin >>= bNextShift;
        }
        query = query + ")";

        return query;
    }

    /**
     * @param end           end
     * @param selfContained self contained
     * @param start         start
     * @param table         table
     * @return Add clause that will restrict to relevant bins to query.
     */
    private static String hAddBinToQueryStandard( String table, long start, long end, boolean selfContained ) {
        int bFirstShift = SequenceBinUtils._binFirstShift, bNextShift = SequenceBinUtils._binNextShift;
        long startBin = ( start >> bFirstShift ), endBin = ( ( end - 1 ) >> bFirstShift );
        int i, levels = SequenceBinUtils.binOffsets.length;

        String column = table + ".bin";

        String query = "";
        if ( selfContained ) {
            query = query + " (";
        }
        for ( i = 0; i < levels; ++i ) {
            int offset = SequenceBinUtils.binOffset( i );
            if ( i != 0 ) {
                query = query + " or ";
            }

            query = SequenceBinUtils.getClause( startBin, endBin, column, query, offset );

            startBin >>= bNextShift;
            endBin >>= bNextShift;
        }
        if ( selfContained ) {
            query = query + " or  " + column + " = " + SequenceBinUtils._binOffsetOldToExtended;
        }
        query = query + ")";
        return query;
    }

    private static int binFromRangeParam( int start, int end, int[] offsets, int oldOffset ) {
        int startBin = start, endBin = end - 1, i;
        startBin >>= SequenceBinUtils._binFirstShift;
        endBin >>= SequenceBinUtils._binFirstShift;
        for ( i = 0; i < offsets.length; ++i ) {
            if ( startBin == endBin )
                return oldOffset + offsets[i] + startBin;
            startBin >>= SequenceBinUtils._binNextShift;
            endBin >>= SequenceBinUtils._binNextShift;
        }
        throw new IllegalArgumentException( "start " + start + ", end " + end + " out of range (max is 512M)" );
    }

}
