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

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.BlatResultService;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * @author pavlidis
 */
public class BlatResultServiceTest extends BaseSpringContextTest {

    private BioSequence bs;

    @Autowired
    BlatResultService blatResultService;

    @Before
    public void setUp() throws Exception {

        for ( int i = 0; i < 20; i++ ) {
            this.bs = this.getTestPersistentBioSequence();
            this.getTestPersistentBlatResult( bs );
        }

    }

    @Test
    public final void testFindBlatResultByBioSequence() {

        Collection<BlatResult> res = this.blatResultService.findByBioSequence( bs );
        assertEquals( "Failed to find blat result for sequence: " + bs, 1, res.size() );
    }

}
