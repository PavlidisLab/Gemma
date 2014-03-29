/*
 * The gemma-model project
 * 
 * Copyright (c) 2013 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.analysis.expression.coexpression;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Paul
 * @version $Id$
 */
public class GeneCoexpressionTestedInTest {

    private static Logger log = LoggerFactory.getLogger( GeneCoexpressionTestedInTest.class );

    @Test
    public void test() {
        IdArray f = new GeneCoexpressionTestedIn( 1L );
        f.addEntity( 1094L );
        assertEquals( 1, f.getNumIds() );
        assertTrue( f.isIncluded( 1094L ) );
        f.removeEntity( 1094L );
        assertTrue( !f.isIncluded( 1094L ) );
        assertEquals( 0, f.getNumIds() );

    }

    @Test
    public void testSpeedA() {
        IdArray f = new GeneCoexpressionTestedIn( 1L );
        int n = 1000;
        for ( long l = 1; l <= n; l++ ) {
            f.addEntity( l );
            if ( l % ( n / 10 ) == 0 ) {
                log.info( "added " + l );
            }
        }
        assertEquals( n, f.getNumIds() );
        for ( long l = 1; l <= n; l++ ) {
            f.removeEntity( l );
            if ( l % ( n / 10 ) == 0 ) {
                log.info( "removed " + l );
            }
        }
        assertEquals( 0, f.getNumIds() );
    }

    @Test
    public void testSpeedB() {
        Random r = new Random();
        IdArray f = new GeneCoexpressionTestedIn( 1L );
        int n = 1000;
        for ( long l = 1; l <= n; l++ ) {
            long g = Math.abs( r.nextInt( n ) + 1L );
            f.addEntity( g );
            if ( l % ( n / 10 ) == 0 ) {
                log.info( "added " + l );
            }
        }

        for ( long l = 1; l <= n; l++ ) {
            f.removeEntity( l );
            if ( l % ( n / 10 ) == 0 ) {
                log.info( "removed " + l );
            }
        }
        assertEquals( 0, f.getNumIds() );
    }
}
