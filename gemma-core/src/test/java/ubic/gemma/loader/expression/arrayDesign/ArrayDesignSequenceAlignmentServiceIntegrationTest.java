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

import java.util.Collection;

import ubic.gemma.apps.Blat;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.util.ConfigUtils;

/**
 * This test will not run unless you have a blat server accessible.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignSequenceAlignmentServiceIntegrationTest extends AbstractArrayDesignProcessingTest {
    Blat blat = new Blat();

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.testing.BaseSpringContextTest#onSetUp()
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void onSetUp() throws Exception {
        super.onSetUp();
        // blat.startServer( BlattableGenome.HUMAN, ConfigUtils.getInt( "gfClient.humanServerPort" ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.test.AbstractDependencyInjectionSpringContextTests#onTearDown()
     */
    @Override
    protected void onTearDown() throws Exception {
        super.onTearDown();
        // blat.stopServer( ConfigUtils.getInt( "gfClient.humanServerPort" ) );
    }

    /**
     * Test method for
     * {@link ubic.gemma.loader.expression.arrayDesign.ArrayDesignSequenceAlignmentService#processArrayDesign(ubic.gemma.model.expression.arrayDesign.ArrayDesign, ubic.gemma.model.genome.Taxon)}.
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

        Taxon taxon = ( ( TaxonService ) getBean( "taxonService" ) ).findByScientificName( "Homo sapiens" );
        ArrayDesignSequenceAlignmentService aligner = ( ArrayDesignSequenceAlignmentService ) getBean( "arrayDesignSequenceAlignmentService" );
        Collection<BlatResult> blatResults = aligner.processArrayDesign( ad, taxon );
        assertEquals( 2, blatResults.size() );

    }

}
