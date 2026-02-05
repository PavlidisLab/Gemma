/*
 * The Gemma project
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
package ubic.gemma.model.genome;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Tests of 'equals' implementation
 *
 * @author pavlidis
 */
public class GeneTest {

    private Taxon aTax;
    private Taxon bTax;
    private PhysicalLocation aLoc;

    @Test
    public void testEqualsA() {
        Gene a = Gene.Factory.newInstance();
        Gene b = Gene.Factory.newInstance();

        a.setOfficialSymbol( "foo" );
        b.setOfficialSymbol( "foo" );

        assertNotEquals( a, b );

    }

    @Test
    public void testEqualsB() {
        Gene a = Gene.Factory.newInstance();
        Gene b = Gene.Factory.newInstance();

        a.setNcbiGeneId( 1234 );
        b.setNcbiGeneId( 1234 );

        assertEquals( a, b );

    }

    @Test
    public void testEqualsC() {
        Gene a = Gene.Factory.newInstance();
        Gene b = Gene.Factory.newInstance();

        a.setOfficialSymbol( "foo" );
        b.setOfficialSymbol( "foo" );
        a.setOfficialName( "foo" );
        b.setOfficialName( "foo" );
        a.setTaxon( aTax );
        b.setTaxon( bTax );

        assertNotEquals( a, b );

    }

    @Test
    public void testEqualsD() {
        Gene a = Gene.Factory.newInstance();
        Gene b = Gene.Factory.newInstance();

        a.setOfficialSymbol( "foo" );
        b.setOfficialSymbol( "foo" );
        a.setOfficialName( "foo" );
        b.setOfficialName( "foo" );
        a.setTaxon( aTax );
        b.setTaxon( aTax );

        assertEquals( a, b );

    }

    @Test
    public void testEqualsE() {
        Gene a = Gene.Factory.newInstance();
        Gene b = Gene.Factory.newInstance();

        a.setTaxon( aTax );
        b.setTaxon( aTax );
        a.setOfficialSymbol( "foo" );
        b.setOfficialSymbol( "foo" );
        a.setOfficialName( "foo" );
        b.setOfficialName( "foo" );
        a.setPhysicalLocation( aLoc );
        b.setPhysicalLocation( aLoc );

        assertEquals( a, b );

    }

    @Before
    public void setUp() throws Exception {
        aTax = Taxon.Factory.newInstance();
        aTax.setScientificName( "Foobius Barius" );
        aTax.setIsGenesUsable( true );
        bTax = Taxon.Factory.newInstance();
        bTax.setScientificName( "Barioobius foobarius" );
        bTax.setIsGenesUsable( true );
        Chromosome c = Chromosome.Factory.newInstance( "X", aTax );

        aLoc = PhysicalLocation.Factory.newInstance();
        aLoc.setChromosome( c );
        aLoc.setNucleotide( 10L );

        PhysicalLocation bLoc = PhysicalLocation.Factory.newInstance();
        bLoc.setChromosome( c );
        bLoc.setNucleotide( 20L );
    }
}
