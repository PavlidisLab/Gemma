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
package ubic.gemma.loader.expression.arrayDesign;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.zip.GZIPInputStream;

import ubic.gemma.apps.Blat;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.util.ConfigUtils;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignProbeMapperServiceIntegrationTest extends AbstractArrayDesignProcessingTest {

    Blat blat = new Blat();

    @Override
    protected void onTearDownInTransaction() throws Exception {
        super.onTearDownInTransaction();
        // TODO: delete blat results that were loaded.
    }

    /**
     * Test method for
     * {@link ubic.gemma.loader.expression.arrayDesign.ArrayDesignProbeMapperService#processArrayDesign(ubic.gemma.model.expression.arrayDesign.ArrayDesign, ubic.gemma.model.genome.Taxon)}.
     */
    public final void testProcessArrayDesign() throws Exception {
        ArrayDesignSequenceProcessingService app = ( ArrayDesignSequenceProcessingService ) getBean( "arrayDesignSequenceProcessingService" );

        try {
            app.processArrayDesign( ad, new String[] { "testblastdb", "testblastdbPartTwo" }, ConfigUtils
                    .getString( "gemma.home" )
                    + "/gemma-core/src/test/resources/data/loader/genome/blast" );
        } catch ( IllegalStateException e ) {
            if ( e.getMessage().startsWith( "No fastacmd executable:" ) ) {
                return;
            }
        }

        ArrayDesignProbeMapperService arrayDesignProbeMapperService = ( ArrayDesignProbeMapperService ) this
                .getBean( "arrayDesignProbeMapperService" );

        // see also the ArrayDesignSequenceAlignementTest.
        Taxon taxon = ( ( TaxonService ) getBean( "taxonService" ) ).findByScientificName( "Homo sapiens" );
        ArrayDesignSequenceAlignmentService aligner = ( ArrayDesignSequenceAlignmentService ) getBean( "arrayDesignSequenceAlignmentService" );

        try {
            aligner.processArrayDesign( ad, taxon );
        } catch ( RuntimeException e ) {
            Throwable ec = e.getCause();
            if ( ec instanceof IOException && ec.getMessage().startsWith( "No bytes available" ) ) {
                // blat presumably isn't running.
                log.warn( "Blat server not available? Skipping test" );
                return;
            }
        }

        // real stuff.
        arrayDesignProbeMapperService.processArrayDesign( ad, taxon );

    }

    /**
     * This test uses 'real' data. 
     * 
     * @throws Exception
     */
    public final void testProcessArrayDesignWithData() throws Exception {

        // possibly insert the needed genes and geneproducts into the system.(can use NCBI gene loader, but for subset)
        
        Taxon taxon = ( ( TaxonService ) getBean( "taxonService" ) ).findByScientificName( "Homo sapiens" );

        // needed to fill in the sequence information for blat scoring.
        InputStream sequenceFile = this.getClass().getResourceAsStream( "/data/loader/genome/gpl140.sequences.fasta" );
        ArrayDesignSequenceProcessingService app = ( ArrayDesignSequenceProcessingService ) getBean( "arrayDesignSequenceProcessingService" );
        app.processArrayDesign( ad, sequenceFile, SequenceType.EST, taxon );

        // fill in the blat results. Note that each time you run this test you get the results loaded again (so they
        // pile up)
        ArrayDesignSequenceAlignmentService aligner = ( ArrayDesignSequenceAlignmentService ) getBean( "arrayDesignSequenceAlignmentService" );

        InputStream blatResultInputStream = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/genome/gpl140.blatresults.psl.gz" ) );

        Collection<BlatResult> results = blat.processPsl( blatResultInputStream, taxon );

        aligner.processArrayDesign( ad, results, taxon );

        // real stuff.
        ArrayDesignProbeMapperService arrayDesignProbeMapperService = ( ArrayDesignProbeMapperService ) this
                .getBean( "arrayDesignProbeMapperService" );
        arrayDesignProbeMapperService.processArrayDesign( ad, taxon );
         
        // possibly assert no unexpected new genes or gene products were added.

    }
}
