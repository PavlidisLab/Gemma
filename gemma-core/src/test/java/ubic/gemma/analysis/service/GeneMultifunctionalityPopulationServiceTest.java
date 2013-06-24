/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
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

package ubic.gemma.analysis.service;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.Multifunctionality;
import ubic.gemma.ontology.providers.GeneOntologyService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author paul
 * @version $Id$
 */
public class GeneMultifunctionalityPopulationServiceTest extends BaseSpringContextTest {

    @Autowired
    private GeneMultifunctionalityPopulationService s;

    @Autowired
    private Gene2GOAssociationService gene2GoService;

    @Autowired
    private GeneOntologyService goService;

    @Autowired
    private GeneService geneService;

    private Taxon testTaxon;

    private String[] goTerms = new String[] { "GO_0001726", "GO_0007049", "GO_0016874", "GO_0005759", "GO_0071681" };

    @After
    public void tearDown() {
        gene2GoService.removeAll();
        if ( testTaxon != null ) {
            Collection<Gene> genes = geneService.loadAll( testTaxon );
            if ( !genes.isEmpty() ) geneService.remove( genes );
        }
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {

        if ( !goService.isRunning() ) {
            goService.init( true );
        }

        int c = 0;
        while ( !goService.isReady() ) {
            Thread.sleep( 5000 );
            log.info( "Waiting for GO to load" );
            if ( ++c > 30 ) {
                fail( "GO loading timeout" );
            }
        }

        testTaxon = taxonService.findOrCreate( Taxon.Factory.newInstance( "foobly", "doobly", "bar", "fo", "fo", 9999,
                true, true, null, null, null ) );

        log.info( "Cleaning ..." );

        gene2GoService.removeAll();
        Collection<Gene> oldGenes = geneService.loadAll( testTaxon );
        if ( !oldGenes.isEmpty() ) geneService.remove( oldGenes );

        log.info( "Creating new genes  ..." );

        /*
         * Create genes
         */
        Collection<Gene> genes = new HashSet<Gene>();
        for ( int i = 0; i < 120; i++ ) {
            Gene gene = getTestPeristentGene( testTaxon );
            genes.add( gene );

            // Some genes get no terms.
            if ( i >= 100 ) continue;

            /*
             * Add up to 5 GO terms. Parents mean more will be added.
             */
            for ( int j = 0; j <= Math.floor( i / 20 ); j++ ) {
                Gene2GOAssociation g2Go1 = Gene2GOAssociation.Factory.newInstance();
                VocabCharacteristic oe = VocabCharacteristic.Factory.newInstance();
                oe.setValueUri( GeneOntologyService.BASE_GO_URI + goTerms[j] );
                oe.setValue( goTerms[j] );
                g2Go1.setOntologyEntry( oe );
                g2Go1.setGene( gene );
                gene2GoService.create( g2Go1 );
            }
        }
        log.info( "Done with setup" );
    }

    @Test
    public void test() {
        log.info( "Updating multifunctionality" );
        s.updateMultifunctionality( testTaxon );

        log.info( "Checking results" );

        Collection<Gene> genes = geneService.loadAll( testTaxon );

        genes = geneService.thawLite( genes );

        assertEquals( 120, genes.size() );

        for ( Gene gene : genes ) {
            Multifunctionality mf = gene.getMultifunctionality();
            if ( mf == null ) continue;

            if ( mf.getNumGoTerms() == 25 ) {
                assertEquals( 0.7458, mf.getRank(), 0.001 );
            }

        }

    }

}
