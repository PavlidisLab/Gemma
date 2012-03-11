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

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import ubic.gemma.apps.Blat;
import ubic.gemma.loader.genome.SimpleFastaCmd;
import ubic.gemma.util.ConfigUtils;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignProbeMapperServiceIntegrationTest extends AbstractArrayDesignProcessingTest {

    Blat blat;

    @Before
    public void setup() throws Exception {
        blat = new Blat();
    }

    /**
     * Test method for
     * {@link ubic.gemma.loader.expression.arrayDesign.ArrayDesignProbeMapperServiceImpl#processArrayDesign(ubic.gemma.model.expression.arrayDesign.ArrayDesign, ubic.gemma.model.genome.Taxon)}
     * .
     */
    @Test
    public final void testProcessArrayDesign() throws Exception {
        if ( !fastaCmdExecutableExists() ) return;
        if ( ad == null ) return;
        ArrayDesignSequenceProcessingService app = ( ArrayDesignSequenceProcessingService ) getBean( "arrayDesignSequenceProcessingService" );

        try {
            ad = this.arrayDesignService.thaw( ad );
            app.processArrayDesign( ad, new String[] { "testblastdb", "testblastdbPartTwo" },
                    ConfigUtils.getString( "gemma.home" ) + "/gemma-core/src/test/resources/data/loader/genome/blast",
                    false );
        } catch ( IllegalStateException e ) {
            if ( e.getMessage().startsWith( "No fastacmd executable:" ) ) {
                return;
            }
        }

        ArrayDesignProbeMapperService arrayDesignProbeMapperService = ( ArrayDesignProbeMapperService ) this
                .getBean( "arrayDesignProbeMapperService" );

        ArrayDesignSequenceAlignmentService aligner = ( ArrayDesignSequenceAlignmentService ) getBean( "arrayDesignSequenceAlignmentService" );

        try {
            aligner.processArrayDesign( ad );
        } catch ( RuntimeException e ) {
            Throwable ec = e.getCause();
            if ( ec instanceof IOException && ec.getMessage().startsWith( "No bytes available" ) ) {
                // blat presumably isn't running.
                log.warn( "Blat server not available? Skipping test" );
                return;
            }
        }

        // real stuff.
        arrayDesignProbeMapperService.processArrayDesign( ad );

    }

    private boolean fastaCmdExecutableExists() {
        String fastacmdExe = ConfigUtils.getString( SimpleFastaCmd.FASTA_CMD_ENV_VAR );
        if ( fastacmdExe == null ) {
            log.warn( "No fastacmd executable is configured, skipping test" );
            return false;
        }

        File fi = new File( fastacmdExe );
        if ( !fi.canRead() ) {
            log.warn( fastacmdExe + " not found, skipping test" );
            return false;
        }
        return true;
    }
}
