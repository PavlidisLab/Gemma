/*
 * The linkAnalysis project
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
package ubic.gemma.analysis.linkAnalysis;

import java.sql.SQLException;
import ubic.basecode.db.Handle;

/**
 * Use to insert coexpression links into the Tmm database. Institution:: Columbia University
 * 
 * @author not attributable
 * @version $Id$
 */
public class LinkInserter {

    private Handle db;

    public LinkInserter() throws SQLException {
        db = new Handle();
    }

    public LinkInserter( Handle h ) {
        db = h;
    }

    /**
     * Insert a single link into the database. DO NOT use this to insert lots of links. Use insertBulkLink instead.
     * 
     * @param p1 The first probe
     * @param p2 The second probe
     * @param ds The data set id
     * @param scorebyte The score, in compressed byte form
     * @param pvalbyte The p value for the score, in compressed byte form.
     * @return Number of rows modified (hopefully 1)
     * @throws SQLException
     */
    public int insertLink( int ds, int p1, int p2, int scorebyte, int pvalbyte ) throws SQLException {
        if ( p1 < p2 ) {
            return db.runUpdateQuery( "INSERT INTO linktemp VALUES(" + ds + "," + p1 + "," + p2 + "," + scorebyte + ","
                    + pvalbyte + ")" );
        } else {
            return db.runUpdateQuery( "INSERT INTO linktemp VALUES(" + ds + "," + p2 + "," + p1 + "," + scorebyte + ","
                    + pvalbyte + ")" );
        }
    }

    /**
     * Same as insertLink, but does it in bulk. Current implementation uses SQL syntax that allows multiple inserts per
     * INSERT command. Current implementation creates a temporary table for the links for performance reasons, and
     * copies the links over at the end. In testing this achieves about 100,000 inserts per second.
     * 
     * @param ds Data set id
     * @param p1 Probe 1 id
     * @param p2 Probe 2 id
     * @param scorebyte Score (e.g., correlation) in byte-compressed format
     * @param pvalbyte P value in byte-compressed format
     * @return Number of rows inserted
     * @throws SQLException if anything goes wrong.
     */
    public int insertBulkLink( int ds, int[] p1, int[] p2, int[] scorebyte, int[] pvalbyte ) throws SQLException {

        // moderate sanity check. This is really a programming error if this happens.
        if ( p1.length != p2.length ) {
            throw new IllegalArgumentException( "Arrays were not the same length." );
        }

        int rowsdone = 0;
        StringBuffer query = new StringBuffer( 10000 );

        db.runUpdateQuery( "CREATE TEMPORARY TABLE link_temp (d SMALLINT UNSIGNED NOT NULL,"
                + "p1 MEDIUMINT UNSIGNED NOT NULL, p2 MEDIUMINT UNSIGNED NOT NULL, s "
                + "SMALLINT UNSIGNED NOT NULL, pv SMALLINT UNSIGNED)" );

        query.append( "INSERT INTO link_temp VALUES " );

        db.runUpdateQuery( "SET AUTOCOMMIT=0" ); // not sure this does anything...

        for ( int i = 0, n = p1.length; i < n; i++ ) {

            if ( p1[i] == 0 ) {
                continue; // in case it wasn't filled in.
            }

            if ( p1[i] < p2[i] ) {
                query.append( "(" + ds + "," + p1[i] + "," + p2[i] + "," + scorebyte[i] + "," + pvalbyte[i] + ")" );
            } else {
                query.append( "(" + ds + "," + p2[i] + "," + p1[i] + "," + scorebyte[i] + "," + pvalbyte[i] + ")" );
            }

            if ( i == n - 1 || ( i % 10000 == 0 && i > 0 ) ) { // send the query every so often.
                rowsdone += db.runUpdateQuery( query.toString() );
                query.delete( 0, query.length() );
                if ( i % 200000 == 0 ) {
                    db.runUpdateQuery( "COMMIT" );
                    System.err.print( "." ); // status.
                }
                query.append( "INSERT INTO link_temp VALUES " ); // start over...
            } else {
                query.append( "," );
            }
        }
        rowsdone = db.runUpdateQuery( "INSERT IGNORE INTO linktemp SELECT * FROM link_temp" );
        db.runUpdateQuery( "DROP TABLE link_temp" );
        db.runUpdateQuery( "SET AUTOCOMMIT=1" );
        return rowsdone;
    }

}