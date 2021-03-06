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

import org.junit.Assert;
import junit.framework.TestCase;

/**
 * Tests of 'equals' implementation
 *
 * @author pavlidis
 */
public class GeneTest extends TestCase {

    private Taxon aTax;
    private Taxon bTax;
    private PhysicalLocation aLoc;

    public void testEqualsA() {
        Gene a = Gene.Factory.newInstance();
        Gene b = Gene.Factory.newInstance();

        a.setOfficialSymbol( "foo" );
        b.setOfficialSymbol( "foo" );

        Assert.assertFalse( a.equals( b ) );

    }

    public void testEqualsB() {
        Gene a = Gene.Factory.newInstance();
        Gene b = Gene.Factory.newInstance();

        a.setNcbiGeneId( 1234 );
        b.setNcbiGeneId( 1234 );

        Assert.assertEquals( a, b );

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

        Assert.assertFalse( a.equals( b ) );

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

        Assert.assertEquals( a, b );

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

        Assert.assertEquals( a, b );

    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        aTax = Taxon.Factory.newInstance();
        aTax.setScientificName( "Foobius Barius" );
        aTax.setIsGenesUsable( true );
        bTax = Taxon.Factory.newInstance();
        bTax.setScientificName( "Barioobius foobarius" );
        bTax.setIsGenesUsable( true );
        Chromosome c = new Chromosome( "X", aTax );

        aLoc = PhysicalLocation.Factory.newInstance();
        aLoc.setChromosome( c );
        aLoc.setNucleotide( 10L );

        PhysicalLocation bLoc = PhysicalLocation.Factory.newInstance();
        bLoc.setChromosome( c );
        bLoc.setNucleotide( 20L );
    }
}
