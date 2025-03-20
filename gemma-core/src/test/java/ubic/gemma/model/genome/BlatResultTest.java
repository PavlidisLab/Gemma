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
package ubic.gemma.model.genome;

import junit.framework.TestCase;
import ubic.gemma.core.analysis.sequence.BlatAssociationScorer;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

/**
 * @author pavlidis
 */
public class BlatResultTest extends TestCase {

    private BlatResult brtest = null;

    public void testScore() {
        brtest.setRepMatches( 0 );
        brtest.setMatches( 49 );
        brtest.setQueryGapCount( 0 );
        brtest.setTargetGapCount( 2 );
        brtest.setMismatches( 1 );
        brtest.setQuerySequence( BioSequence.Factory.newInstance() );
        brtest.getQuerySequence().setLength( 50L );
        double actualReturn = BlatAssociationScorer.score( brtest );
        double expectedReturn = 47.0 / 50.0;
        TestCase.assertEquals( expectedReturn, actualReturn, 0.001 );
    }

    public void testScorewr() {
        brtest.setRepMatches( 2 );
        brtest.setMatches( 47 );
        brtest.setQueryGapCount( 0 );
        brtest.setTargetGapCount( 2 );
        brtest.setMismatches( 1 );
        brtest.setQuerySequence( BioSequence.Factory.newInstance() );
        brtest.getQuerySequence().setLength( 50L );
        double actualReturn = BlatAssociationScorer.score( brtest );
        double expectedReturn = 47.0 / 50.0;
        TestCase.assertEquals( expectedReturn, actualReturn, 0.001 );
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        brtest = BlatResult.Factory.newInstance();

    }

}
