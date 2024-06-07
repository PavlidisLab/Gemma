/*
 * The gemma project
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
package ubic.gemma.model.expression.coexpression;

import lombok.extern.apachecommons.CommonsLog;
import org.junit.Test;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionTestedIn;
import ubic.gemma.model.analysis.expression.coexpression.IdArray;

import java.util.Collection;
import java.util.HashSet;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Paul
 */
@CommonsLog
public class GeneCoexpressionTestedInTest {

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
    public void testBulkAdd() {
        IdArray f = new GeneCoexpressionTestedIn( 1L );

        Collection<Long> toAdd = new HashSet<>();
        toAdd.add( 5L );
        toAdd.add( 15L );
        toAdd.add( 2L );
        toAdd.add( 235L );
        toAdd.add( 775L );
        toAdd.add( 54L );
        toAdd.add( 3L );
        toAdd.add( 23L );
        toAdd.add( 98L );

        f.addEntities( toAdd );
        f.addEntity( 98L );
        f.addEntity( 1000L );
        assertTrue( f.isIncluded( 98L ) );
        assertTrue( f.isIncluded( 1000L ) );
        assertTrue( f.isIncluded( 5L ) );
        assertTrue( f.isIncluded( 15L ) );
        assertTrue( f.isIncluded( 2L ) );
        assertTrue( f.isIncluded( 235L ) );
        assertTrue( f.isIncluded( 775L ) );
        assertTrue( f.isIncluded( 54L ) );
        assertTrue( f.isIncluded( 3L ) );
        assertTrue( f.isIncluded( 23L ) );

    }

    @Test
    public void testSpeedA() {
        IdArray f = new GeneCoexpressionTestedIn( 1L );
        int n = 1000;
        for ( long l = 1; l <= n; l++ ) {
            f.addEntity( l );
            if ( l % ( n / 10 ) == 0 ) {
                GeneCoexpressionTestedInTest.log.info( "added " + l );
            }
        }
        assertEquals( n, f.getNumIds() );
        for ( long l = 1; l <= n; l++ ) {
            f.removeEntity( l );
            if ( l % ( n / 10 ) == 0 ) {
                GeneCoexpressionTestedInTest.log.info( "removed " + l );
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
                GeneCoexpressionTestedInTest.log.info( "added " + l );
            }
        }

        for ( long l = 1; l <= n; l++ ) {
            f.removeEntity( l );
            if ( l % ( n / 10 ) == 0 ) {
                GeneCoexpressionTestedInTest.log.info( "removed " + l );
            }
        }
        assertEquals( 0, f.getNumIds() );
    }
}
