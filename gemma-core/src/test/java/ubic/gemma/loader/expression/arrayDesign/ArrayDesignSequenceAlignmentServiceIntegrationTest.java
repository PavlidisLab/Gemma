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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.junit.Test;

import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.util.ConfigUtils;

/**
 * This test will not run unless you have a blat server accessible.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignSequenceAlignmentServiceIntegrationTest extends AbstractArrayDesignProcessingTest {

    /**
     * Test method for
     * {@link ubic.gemma.loader.expression.arrayDesign.ArrayDesignSequenceAlignmentServiceImpl#processArrayDesign(ubic.gemma.model.expression.arrayDesign.ArrayDesign, ubic.gemma.model.genome.Taxon)}
     * .
     */
    @Test
    public final void testProcessArrayDesign() throws Exception {
        if ( ad == null ) return;
        String gfClientExe = ConfigUtils.getString( "gfClient.exe" );

        if ( gfClientExe == null ) {
            log.warn( "No gfClient executable is configured, skipping test" );
            return;
        }

        File fi = new File( gfClientExe );
        if ( !fi.canRead() ) {
            log.warn( gfClientExe + " not found, skipping test" );
            return;
        }

        ArrayDesignSequenceProcessingService app = ( ArrayDesignSequenceProcessingService ) getBean( "arrayDesignSequenceProcessingService" );
        ad = arrayDesignService.thaw( ad );
        try {
            app.processArrayDesign( ad, new String[] { "testblastdb", "testblastdbPartTwo" }, ConfigUtils
                    .getString( "gemma.home" )
                    + "/gemma-core/src/test/resources/data/loader/genome/blast", false );

        } catch ( IllegalStateException e ) {
            if ( e.getMessage().startsWith( "No fastacmd executable:" ) ) {
                return;
            }
        }

        ArrayDesignSequenceAlignmentService aligner = ( ArrayDesignSequenceAlignmentService ) getBean( "arrayDesignSequenceAlignmentService" );

        try {
            Collection<BlatResult> blatResults = aligner.processArrayDesign( ad );
            assertEquals( 2, blatResults.size() );
        } catch ( RuntimeException e ) {
            Throwable ec = e.getCause();
            if ( ec instanceof IOException && ec.getMessage().startsWith( "No bytes available" ) ) {
                // blat presumably isn't running.
                log.warn( "Blat server not available? Skipping test" );
                return;
            }
            throw e;
        }

    }

}
