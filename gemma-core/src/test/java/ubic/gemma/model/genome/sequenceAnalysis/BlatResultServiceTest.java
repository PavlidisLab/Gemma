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
package ubic.gemma.model.genome.sequenceAnalysis;

import static org.junit.Assert.assertEquals;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class BlatResultServiceTest extends BaseSpringContextTest {

    BioSequence bs;

    @Autowired
    BlatResultService blatResultService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.testing.BaseTransactionalSpringContextTest#onSetUpInTransaction()
     */
    @Before
    public void setup() {

        for ( int i = 0; i < 20; i++ ) {
            this.bs = this.getTestPersistentBioSequence();
            this.getTestPersistentBlatResult( bs );
        }

    }

    /**
     * Test method for
     * {@link ubic.gemma.model.genome.sequenceAnalysis.BlatResultServiceImpl#find(ubic.gemma.model.genome.biosequence.BioSequence)}
     * .
     */
    @Test
    public final void testFindBlatResultByBioSequence() {

        Collection<BlatResult> res = this.blatResultService.findByBioSequence( bs );
        assertEquals( "Failed to find blat result for sequence: " + bs, 1, res.size() );
    }

}
