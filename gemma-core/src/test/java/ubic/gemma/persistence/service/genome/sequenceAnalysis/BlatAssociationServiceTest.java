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
package ubic.gemma.persistence.service.genome.sequenceAnalysis;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.service.genome.gene.GeneProductService;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.BlatAssociationService;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.BlatResultService;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author pavlidis
 */
public class BlatAssociationServiceTest extends BaseSpringContextTest {

    private final String testGeneIdentifier = RandomStringUtils.insecure().nextAlphabetic( 4 );

    private final String testSequence = RandomStringUtils.insecure().next( 35, "ATGC" );
    private final String testSequenceName = RandomStringUtils.insecure().nextAlphabetic( 6 );

    @Autowired
    private BlatAssociationService blatAssociationService;

    @Autowired
    private BlatResultService blatResultService;

    @Autowired
    private BioSequenceService bioSequenceService;

    @Autowired
    private GeneProductService geneProductService;

    @Autowired
    private GeneService geneService;

    @Before
    public void setUp() throws Exception {

        int numSequencesToCreate = 20;
        for ( int i = 0; i < numSequencesToCreate; i++ ) {
            BioSequence bs = this.getTestPersistentBioSequence();
            if ( i == 11 ) {
                bs.setSequence( testSequence );
                bs.setName( testSequenceName );
                this.bioSequenceService.update( bs );
            }

            BlatResult br = this.getTestPersistentBlatResult( bs );
            br.setQuerySequence( bs );
            blatResultService.update( br );

            BlatAssociation ba = BlatAssociation.Factory.newInstance();

            Gene g = this.getTestPersistentGene();

            GeneProduct gp = this.getTestPersistentGeneProduct( g );
            if ( i == 10 ) {
                g.setOfficialName( testGeneIdentifier );
                gp.setGene( g );
                gp.setName( testGeneIdentifier );
                this.geneProductService.update( gp );
            }

            ba.setGeneProduct( gp );
            ba.setBlatResult( br );
            ba.setBioSequence( bs );

            blatAssociationService.create( ba );

        }
    }

    @After
    public void tearDown() {
        Collection<Gene> genes = geneService.loadAll();
        for ( Gene gene : genes ) {
            try {
                geneService.remove( gene );
            } catch ( Exception ignored ) {

            }
        }
    }

    @Test
    @Category( SlowTest.class )
    public final void testFindBioSequence() {
        BioSequence bs = BioSequence.Factory.newInstance();
        Taxon t = Taxon.Factory.newInstance();
        t.setScientificName( "Mus musculus" ); // has to match what the testpersistent object is.
        t.setIsGenesUsable( true );
        bs.setSequence( testSequence );
        bs.setTaxon( t );
        bs.setName( testSequenceName );

        BioSequence bsIn = this.bioSequenceService.find( bs );
        assertNotNull( "Did not find " + bs, bsIn );

        Collection<BlatAssociation> res = this.blatAssociationService.find( bs );
        assertEquals( "Was seeking blatresults for sequence " + testSequenceName, 1, res.size() );
    }

    @Test
    @Category(SlowTest.class)
    public final void testFindGene() {
        Gene g = Gene.Factory.newInstance();
        g.setOfficialName( testGeneIdentifier );

        GeneProduct gp = GeneProduct.Factory.newInstance();
        gp.setName( testGeneIdentifier );

        g.getProducts().add( gp );
        gp.setGene( g );

        Collection<BlatAssociation> res = this.blatAssociationService.find( g );
        assertEquals( 1, res.size() );
    }

}
