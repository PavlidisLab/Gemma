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
package ubic.gemma.core.loader.expression.arrayDesign;

import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.analysis.sequence.Blat;
import ubic.gemma.core.util.test.category.GoldenPathTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

import java.util.Collection;

import static org.junit.Assert.*;

/**
 * @author pavlidis
 */
@Category(GoldenPathTest.class)
public class ArrayDesignSequenceAlignmentandMappingTest extends AbstractArrayDesignProcessingTest {

    @Autowired
    private ArrayDesignSequenceProcessingService app;

    @Autowired
    private ArrayDesignSequenceAlignmentService aligner;

    @Test
    @Category(SlowTest.class)
    public final void testProcessArrayDesign() throws Exception {

        ad = arrayDesignService.thaw( ad );

        Collection<BioSequence> seqs = app.processArrayDesign( ad, new String[] { "testblastdb", "testblastdbPartTwo" },
                FileTools.resourceToPath( "/data/loader/genome/blast" ), true,
                new MockFastaCmd( ad.getPrimaryTaxon() ) );

        assertNotNull( seqs );
        assertFalse( seqs.isEmpty() );

        Blat mockBlat = new MockBlat( ad.getPrimaryTaxon() );

        ad = arrayDesignService.thaw( ad );

        Collection<BlatResult> blatResults = aligner.processArrayDesign( ad, mockBlat );
        assertTrue( blatResults.size() > 200 );

    }

    @Override
    @After
    public final void tearDown() {
        super.tearDown();
    }
}