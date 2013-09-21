/*
 * The gemma-core project
 * 
 * Copyright (c) 2013 University of British Columbia
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

package ubic.gemma.analysis.sequence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;

import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.CytogeneticLocation;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.util.ChromosomeUtil;

/**
 * {@link BlatAssociationScorer} tests
 * 
 * @author ptan
 * @version $Id$
 */
public class BlatAssociationScorerTest {

    private BlatResult createBlatResult( String name ) {
        BlatResult blatResult = BlatResult.Factory.newInstance();
        blatResult.setRepMatches( 0 );
        blatResult.setMatches( 49 );
        blatResult.setQueryGapCount( 0 );
        blatResult.setTargetGapCount( 2 );
        blatResult.setMismatches( 1 );
        BioSequence sequence = BioSequence.Factory.newInstance();
        blatResult.setQuerySequence( sequence );
        blatResult.getQuerySequence().setLength( 50L );

        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( "human" );

        Chromosome chr = Chromosome.Factory.newInstance(name, taxon);
        blatResult.setTargetChromosome( chr );

        return blatResult;
    }

    private GeneProduct createGeneProduct( String name ) {

        Chromosome chromosome = Chromosome.Factory.newInstance( name, null );
        CytogeneticLocation cytogenicLocation = CytogeneticLocation.Factory.newInstance( chromosome, null );
        GeneProduct geneProduct = GeneProduct.Factory.newInstance();
        Gene gene = Gene.Factory.newInstance();

        geneProduct.setGene( gene );
        geneProduct.setName( name );
        geneProduct.setCytogenicLocation( cytogenicLocation );
        gene.setCytogenicLocation( cytogenicLocation );

        return geneProduct;
    }

    /**
     * Test method for
     * {@link ubic.gemma.analysis.sequence.BlatAssociationScorer#scoreResults(java.util.Collection, ubic.gemma.analysis.sequence.ProbeMapperConfig)}
     * .
     */
    @Test
    public void testScoreResults() {

        // there's only one gene product that is aligned to two different regions
        GeneProduct geneProduct = createGeneProduct( "geneProduct" );

        BlatResult blatResult_1 = createBlatResult( "6_cox_hap2" );
        BlatResult blatResult_2 = createBlatResult( "6" );

        // this has the highest score but located on a non-canonical chromosome
        // so this should be ignored
        BlatAssociation association_1 = BlatAssociation.Factory.newInstance();
        association_1.setGeneProduct( geneProduct );
        association_1.setBlatResult( blatResult_1 );
        association_1.setScore( 50.0 );
        association_1.setOverlap( 50 );
        association_1.setBioSequence( BioSequence.Factory.newInstance() );

        BlatAssociation association_2 = BlatAssociation.Factory.newInstance();
        association_2.setGeneProduct( geneProduct );
        association_2.setBlatResult( blatResult_2 );
        association_2.setScore( 30.0 );
        association_2.setOverlap( 30 );
        association_2.setBioSequence( BioSequence.Factory.newInstance() );

        Collection<BlatAssociation> blatAssociations = new ArrayList<BlatAssociation>();
        blatAssociations.add( association_1 );
        blatAssociations.add( association_2 );

        ProbeMapperConfig config = new ProbeMapperConfig();
        config.setTrimNonCanonicalChromosomeHits( true );

        // BlatAssociation expected = association_2;
        BlatAssociation actual = BlatAssociationScorer.scoreResults( blatAssociations, config );

        assertFalse( ChromosomeUtil.isCanonical( blatResult_1.getTargetChromosome() ) );
        assertTrue( ChromosomeUtil.isCanonical( blatResult_2.getTargetChromosome() ) );
        assertEquals( 940.0, association_1.getScore().doubleValue(), 0 );
        assertEquals( 564.0, association_2.getScore().doubleValue(), 0 );
        assertEquals( 1.0, actual.getSpecificity().doubleValue(), 0 );
        // assertEquals( expected, actual );

    }

}
