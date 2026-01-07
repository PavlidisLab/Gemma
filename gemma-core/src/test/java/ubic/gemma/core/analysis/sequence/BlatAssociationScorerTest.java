/*
 * The gemma project
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

package ubic.gemma.core.analysis.sequence;

import org.junit.Test;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.service.genome.ChromosomeUtils;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * {@link BlatAssociationScorer} tests
 *
 * @author ptan
 */
public class BlatAssociationScorerTest {

    @Test
    public void testScoreResults() {

        // there's only one gene product that is aligned to two different regions
        GeneProduct geneProduct = this.createGeneProduct();
        BioSequence querySequence = BioSequence.Factory.newInstance();

        BlatResult blatResult_1 = this.createBlatResult( "6_cox_hap2" );
        BlatResult blatResult_2 = this.createBlatResult( "6" );

        // this has the highest score but located on a non-canonical chromosome
        // so this should be ignored
        BlatAssociation association_1 = BlatAssociation.Factory.newInstance();
        association_1.setGeneProduct( geneProduct );
        association_1.setBlatResult( blatResult_1 );
        association_1.setScore( 50.0 );
        association_1.setOverlap( 50 );
        association_1.setBioSequence( querySequence );

        BlatAssociation association_2 = BlatAssociation.Factory.newInstance();
        association_2.setGeneProduct( geneProduct );
        association_2.setBlatResult( blatResult_2 );
        association_2.setScore( 30.0 );
        association_2.setOverlap( 30 );
        association_2.setBioSequence( querySequence );

        Collection<BlatAssociation> blatAssociations = new ArrayList<>();
        blatAssociations.add( association_1 );
        blatAssociations.add( association_2 );

        ProbeMapperConfig config = new ProbeMapperConfig();
        config.setTrimNonCanonicalChromosomeHits( true );

        // BlatAssociation expected = association_2;
        BlatAssociation actual = BlatAssociationScorer.scoreResults( blatAssociations );

        assertFalse( ChromosomeUtils.isCanonical( blatResult_1.getTargetChromosome() ) );
        assertTrue( ChromosomeUtils.isCanonical( blatResult_2.getTargetChromosome() ) );
        assertEquals( 940.0, association_1.getScore(), 0 );
        assertEquals( 564.0, association_2.getScore(), 0 );
        assertEquals( 1.0, actual.getSpecificity(), 0 );
        // assertEquals( expected, actual );

    }

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

        Chromosome chr = new Chromosome( name, taxon );
        blatResult.setTargetChromosome( chr );

        return blatResult;
    }

    private GeneProduct createGeneProduct() {

        GeneProduct geneProduct = GeneProduct.Factory.newInstance();
        Gene gene = Gene.Factory.newInstance();

        geneProduct.setGene( gene );
        geneProduct.setName( "geneProduct" );

        return geneProduct;
    }

}
