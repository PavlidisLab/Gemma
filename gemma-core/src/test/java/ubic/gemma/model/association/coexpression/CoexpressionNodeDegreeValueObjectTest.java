/*
 * The gemma project
 * 
 * Copyright (c) 2014 University of British Columbia
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

package ubic.gemma.model.association.coexpression;

import org.junit.Test;
import ubic.gemma.model.genome.Gene;

import static org.junit.Assert.assertEquals;

/**
 * @author Paul
 *
 */
public class CoexpressionNodeDegreeValueObjectTest {

    @Test
    public void testa() {
        Gene g = Gene.Factory.newInstance();
        g.setId( 1L );
        g.setOfficialName( "foo" );
        GeneCoexpressionNodeDegree nd = GeneCoexpressionNodeDegree.Factory.newInstance( g );

        GeneCoexpressionNodeDegreeValueObject t = new GeneCoexpressionNodeDegreeValueObject( nd );
        t.increment( 1, true );
        t.increment( 2, true );

        assertEquals( 2, t.getLinksWithMinimumSupport( 1, true ).intValue() );
        assertEquals( 1, t.getLinksWithExactSupport( 1, true ).intValue() );

        t.increment( 1, true );
        t.increment( 2, true );
        assertEquals( 4, t.getLinksWithMinimumSupport( 1, true ).intValue() );
        assertEquals( 2, t.getLinksWithMinimumSupport( 2, true ).intValue() );
        assertEquals( 2, t.getLinksWithExactSupport( 1, true ).intValue() );

        assertEquals( 3, t.asIntArrayPos().length );
        assertEquals( 0, t.asIntArrayPos()[0] );
        assertEquals( 2, t.asIntArrayPos()[1] );
        assertEquals( 2, t.asIntArrayPos()[2] );

    }

}
