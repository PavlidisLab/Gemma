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
package edu.columbia.gemma.genome.sequenceAnalysis;

import edu.columbia.gemma.genome.biosequence.BioSequence;
import junit.framework.TestCase;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 */
public class BlatResultImplTest extends TestCase {

    BlatResult brtest = null;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        brtest = BlatResult.Factory.newInstance();

    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testScore() {
        brtest.setMatches( 49 );
        brtest.setQueryGapCount( 0 );
        brtest.setTargetGapCount( 2 );
        brtest.setMismatches( 1 );
        brtest.setQuerySequence( BioSequence.Factory.newInstance() );
        brtest.getQuerySequence().setLength( 50 );
        double actualReturn = brtest.score();
        double expectedReturn = 47.0 / 50.0;
        assertEquals( expectedReturn, actualReturn, 0.001 );
    }

}
