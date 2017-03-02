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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.util.FileTools;
import ubic.gemma.apps.Blat;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignSequenceAlignmentandMappingTest extends AbstractArrayDesignProcessingTest {

    @Autowired
    private ArrayDesignSequenceProcessingService app;

    @Autowired
    private ArrayDesignSequenceAlignmentService aligner;

    @Test
    public final void testProcessArrayDesign() throws Exception {

        ad = arrayDesignService.thaw( ad );

        Collection<BioSequence> seqs = app.processArrayDesign( ad, new String[] { "testblastdb", "testblastdbPartTwo" },
                FileTools.resourceToPath( "/data/loader/genome/blast" ), true,
                new MockFastaCmd( ad.getPrimaryTaxon() ) );

        assertNotNull( seqs );
        assertTrue( !seqs.isEmpty() );

        Blat mockBlat = new MockBlat( ad.getPrimaryTaxon() );

        ad = arrayDesignService.thaw( ad );

        Collection<BlatResult> blatResults = aligner.processArrayDesign( ad, mockBlat );
        assertTrue( blatResults.size() > 200 );

    }

    @Override
    @After
    public final void tearDown() {
        super.tearDown();

        // todo delete more stuff.
    }
}