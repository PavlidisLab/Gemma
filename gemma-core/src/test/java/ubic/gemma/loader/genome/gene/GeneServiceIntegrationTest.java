/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.loader.genome.gene;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Collection;
import java.util.zip.GZIPInputStream;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.apps.Blat;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.loader.expression.arrayDesign.ArrayDesignProbeMapperService;
import ubic.gemma.loader.expression.arrayDesign.ArrayDesignProbeMapperServiceIntegrationTest;
import ubic.gemma.loader.expression.arrayDesign.ArrayDesignSequenceAlignmentService;
import ubic.gemma.loader.expression.arrayDesign.ArrayDesignSequenceProcessingService;
import ubic.gemma.loader.expression.geo.service.GeoService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class GeneServiceIntegrationTest extends BaseSpringContextTest {
    ArrayDesignProbeMapperServiceIntegrationTest pTest = new ArrayDesignProbeMapperServiceIntegrationTest();

    String officialName = "PPARA";
    String accession = "GPL140";

    static boolean setupDone = false;

    @Autowired
    ArrayDesignService arrayDesignService;

    @Autowired
    ArrayDesignSequenceProcessingService arrayDesignSequenceProcessingService;

    @Autowired
    ArrayDesignProbeMapperService arrayDesignProbeMapperService;

    @Autowired
    ArrayDesignSequenceAlignmentService arrayDesignSequenceAlignmentService;

    @Before
    public void setup() throws Exception {

        if ( !setupDone ) {
            ArrayDesign ad;
            // first load small twoc-color
            GeoService geoService = this.getBean( GeoService.class );
            final Collection<ArrayDesign> ads = ( Collection<ArrayDesign> ) geoService.fetchAndLoad( accession, true,
                    true, false, false, true, true );
            ad = ads.iterator().next();

            ad = arrayDesignService.thaw( ad );

            Taxon taxon = taxonService.findByScientificName( "Homo sapiens" );

            // needed to fill in the sequence information for blat scoring.
            InputStream sequenceFile = this.getClass().getResourceAsStream(
                    "/data/loader/genome/gpl140.sequences.fasta" );
            arrayDesignSequenceProcessingService.processArrayDesign( ad, sequenceFile, SequenceType.EST );

            // fill in the blat results. Note that each time you run this test you get the results loaded again (so they
            // pile up)

            InputStream blatResultInputStream = new GZIPInputStream( this.getClass().getResourceAsStream(
                    "/data/loader/genome/gpl140.blatresults.psl.gz" ) );

            Blat blat = new Blat();
            Collection<BlatResult> results = blat.processPsl( blatResultInputStream, taxon );

            arrayDesignSequenceAlignmentService.processArrayDesign( ad, taxon, results );

            arrayDesignProbeMapperService.processArrayDesign( ad );
            setupDone = true;
        }
    }

    @Test
    public void testGetCompositeSequenceCountById() {

        // get geneService
        GeneService geneService = this.getBean( GeneService.class );
        // get a gene to get the id
        Collection<Gene> geneCollection = geneService.findByOfficialSymbol( officialName );
        assertTrue( !geneCollection.isEmpty() );
        Gene g = geneCollection.iterator().next();
        assertNotNull( g );
        long count = geneService.getCompositeSequenceCountById( g.getId() );
        assertTrue( count != 0 );
    }

    @Test
    public void testGetCompositeSequencesById() {

        // get geneService
        GeneService geneService = this.getBean( GeneService.class );
        // get a gene to get the id
        Collection<Gene> geneCollection = geneService.findByOfficialSymbol( officialName );
        assertTrue( !geneCollection.isEmpty() );
        Gene g = geneCollection.iterator().next();
        assertNotNull( g );
        Collection<CompositeSequence> compSequences = geneService.getCompositeSequencesById( g.getId() );
        assertTrue( compSequences.size() != 0 );
    }

    // preloads GPL140. See ArrayDesignProbeMapperServiceIntegrationTest

    @Test
    public void testGetGenesByTaxon() {
        // get geneService
        GeneService geneService = this.getBean( GeneService.class );

        Taxon taxon = taxonService.findByCommonName( "human" );
        assertNotNull( taxon );
        Collection<Gene> geneCollection = geneService.getGenesByTaxon( taxon );
        assertTrue( !geneCollection.isEmpty() );

    }
}
