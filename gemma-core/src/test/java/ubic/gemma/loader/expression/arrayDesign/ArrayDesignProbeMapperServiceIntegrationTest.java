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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.apps.Blat;
import ubic.gemma.loader.genome.SimpleFastaCmd;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.util.ConfigUtils;

/**
 * This test relies on having blat actually runnning.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignProbeMapperServiceIntegrationTest extends AbstractArrayDesignProcessingTest {

    Blat blat;
    @Autowired
    ArrayDesignSequenceProcessingService app;
    @Autowired
    ArrayDesignProbeMapperService arrayDesignProbeMapperService;
    @Autowired
    ArrayDesignSequenceAlignmentService aligner;

    @Before
    public void setup() {
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

        try {
            ad = this.arrayDesignService.thaw( ad );
            Collection<BioSequence> sequences = app.processArrayDesign( ad, new String[] { "testblastdb",
                    "testblastdbPartTwo" }, ConfigUtils.getString( "gemma.home" )
                    + "/gemma-core/src/test/resources/data/loader/genome/blast", true );
            assertEquals( 243, sequences.size() );
        } catch ( IllegalStateException e ) {
            if ( e.getMessage().startsWith( "No fastacmd executable:" ) ) {
                return;
            }
        }

        try {
            aligner.processArrayDesign( ad );
        } catch ( RuntimeException e ) {
            Throwable ec = e.getCause();
            if ( ec instanceof IOException && ec.getMessage().startsWith( "No bytes available" ) ) {
                // blat presumably isn't running.
                log.warn( "Blat server not available? Skipping test" );
                return;
            }
            log.error( e, e );
            fail();
        }

        // real stuff.
        arrayDesignProbeMapperService.processArrayDesign( ad );

        Map<CompositeSequence, Collection<BlatResult>> alignments = arrayDesignService.getAlignments( ad );
        assertEquals( 100, alignments.size() );
        for ( CompositeSequence cs : alignments.keySet() ) {
            assertTrue( !alignments.get( cs ).isEmpty() );
        }

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
