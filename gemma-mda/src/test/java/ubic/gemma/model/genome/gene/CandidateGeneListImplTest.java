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
package ubic.gemma.model.genome.gene;

import junit.framework.TestCase;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

/**
 * @author daq2101
 * @version $Id$
 */
public class CandidateGeneListImplTest extends TestCase {

    private Taxon t = null;
    private CandidateGeneList cgl = null;

    private Gene makeGene( String officialName ) {

        Gene g = Gene.Factory.newInstance();
        g.setName( officialName );
        g.setOfficialSymbol( officialName );
        g.setOfficialName( officialName );
        g.setTaxon( t );
        return g;
    }

    protected void setUp() throws Exception {
        super.setUp();
        t = Taxon.Factory.newInstance();
        t.setCommonName( "mouse" );
        t.setScientificName( "mouse" );
    }

    public void testUseCandidateGeneList() {

        Gene gene1 = makeGene( "foo1" );
        Gene gene2 = makeGene( "foo2" );
        Gene gene3 = makeGene( "foo3" );

        cgl = CandidateGeneList.Factory.newInstance();
        cgl.setDescription( "Test my candidate list" );
        cgl.setName( "Test more Candidates" );

        CandidateGene cg1 = cgl.addCandidate( gene1 );
        cg1.setName( "Candidate one" );
        cg1.setDescription( "Candidate One Described" );

        CandidateGene cg2 = cgl.addCandidate( gene2 );
        cg2.setName( "Candidate two" );
        cg2.setDescription( "Candidate Two Described" );

        CandidateGene cg3 = cgl.addCandidate( gene3 );
        cg3.setName( "Candidate three" );
        cg3.setDescription( "Candidate Three Described" );

        assertTrue( cgl.getCandidates().size() == 3 );

        // original ranking is cg1, cg2, cg3
        cgl.decreaseRanking( cg1 );
        cgl.increaseRanking( cg3 );
        // now ranking should be cg2, cg3, cg1
        assertTrue( cg1.getRank().intValue() > cg2.getRank().intValue()
                && cg1.getRank().intValue() > cg3.getRank().intValue() );
        assertTrue( cg2.getRank().intValue() < cg1.getRank().intValue()
                && cg2.getRank().intValue() < cg3.getRank().intValue() );

        // these should have no effect, since the cgs are at the top/bottom
        cgl.increaseRanking( cg2 );
        cgl.decreaseRanking( cg1 );
        assertTrue( cg1.getRank().intValue() > cg2.getRank().intValue()
                && cg1.getRank().intValue() > cg3.getRank().intValue() );
        assertTrue( cg2.getRank().intValue() < cg1.getRank().intValue()
                && cg2.getRank().intValue() < cg3.getRank().intValue() );

        cgl.removeCandidate( cg1 );
        cgl.removeCandidate( cg2 );
        cgl.removeCandidate( cg3 );

        assertTrue( cgl.getCandidates().size() == 0 );
    }
}
