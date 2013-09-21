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

import junit.framework.TestCase;

/**
 * Tests of 'equals' implementation
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeneImplTest extends TestCase {

    Taxon aTax;
    Taxon bTax;
    PhysicalLocation aLoc;
    PhysicalLocation bLoc;

    public void testEqualsA() {
        Gene a = Gene.Factory.newInstance();
        Gene b = Gene.Factory.newInstance();

        a.setOfficialSymbol( "foo" );
        b.setOfficialSymbol( "foo" );

        assertFalse( a.equals( b ) );

    }

    public void testEqualsB() {
        Gene a = Gene.Factory.newInstance();
        Gene b = Gene.Factory.newInstance();

        a.setNcbiGeneId( 1234 );
        b.setNcbiGeneId( 1234 );

        assertEquals( a, b );

    }

    public void testEqualsC() {
        Gene a = Gene.Factory.newInstance();
        Gene b = Gene.Factory.newInstance();

        a.setOfficialSymbol( "foo" );
        b.setOfficialSymbol( "foo" );
        a.setOfficialName( "foo" );
        b.setOfficialName( "foo" );
        a.setTaxon( aTax );
        b.setTaxon( bTax );

        assertFalse( a.equals( b ) );

    }

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

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        aTax = Taxon.Factory.newInstance();
        aTax.setScientificName( "Foobius Barius" );
        aTax.setIsSpecies( true );
        aTax.setIsGenesUsable( true );
        bTax = Taxon.Factory.newInstance();
        bTax.setScientificName( "Barioobius foobarius" );
        bTax.setIsSpecies( true );
        bTax.setIsGenesUsable( true );
        Chromosome c = Chromosome.Factory.newInstance( "X", aTax );

        aLoc = PhysicalLocation.Factory.newInstance();
        aLoc.setChromosome( c );
        aLoc.setNucleotide( 10L );

        bLoc = PhysicalLocation.Factory.newInstance();
        bLoc.setChromosome( c );
        bLoc.setNucleotide( 20L );
    }
}
