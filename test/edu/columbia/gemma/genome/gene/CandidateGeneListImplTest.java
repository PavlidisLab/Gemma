package edu.columbia.gemma.genome.gene;


import edu.columbia.gemma.BaseDAOTestCase;

import edu.columbia.gemma.genome.Taxon;
import edu.columbia.gemma.genome.Gene;

/**
 * <hr>
 * <p>Copyright (c) 2004, 2006 University of British Columbia
 * @author daq2101
 * @version $Id$
 */ 
public class CandidateGeneListImplTest extends BaseDAOTestCase {

    private Taxon t = null;
    private CandidateGeneList cgl = null;
    
    private Gene makeGene(String officialName ){
        
        Gene g = Gene.Factory.newInstance();
		g.setName(officialName);
		g.setOfficialSymbol(officialName);
		g.setOfficialName(officialName);
		g.setTaxon(t);
		return g;
    }
    
    protected void setUp() throws Exception {
        super.setUp();
        t = Taxon.Factory.newInstance();
        t.setCommonName("mouse");
		t.setScientificName("mouse");
    }
    
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testUseCandidateGeneList() {
     
        Gene gene1 = makeGene("foo1");
        Gene gene2 = makeGene("foo2");
        Gene gene3 = makeGene("foo3");
        
        cgl = CandidateGeneList.Factory.newInstance();
		cgl.setDescription("Test my candidate list");
		cgl.setName("Test more Candidates");
		
        CandidateGene cg1 = cgl.addCandidate(gene1);
        cg1.setName("Candidate one");
        cg1.setDescription("Candidate One Described");
        
        CandidateGene cg2 = cgl.addCandidate(gene2);
        cg2.setName("Candidate two");
        cg2.setDescription("Candidate Two Described");

        CandidateGene cg3 = cgl.addCandidate(gene3);
        cg3.setName("Candidate three");
        cg3.setDescription("Candidate Three Described");
       
        assertTrue( cgl.getCandidates().size()==3);
        
        // original ranking is cg1, cg2, cg3
        cgl.decreaseRanking(cg1);
        cgl.increaseRanking(cg3);
        // now ranking should be cg2, cg3, cg1
        assertTrue( cg1.getRank().intValue() > cg2.getRank().intValue() && 
                cg1.getRank().intValue() > cg3.getRank().intValue());
        assertTrue( cg2.getRank().intValue() < cg1.getRank().intValue() && 
                cg2.getRank().intValue() < cg3.getRank().intValue());
        
        // these should have no effect, since the cgs are at the top/bottom
        cgl.increaseRanking(cg2);
        cgl.decreaseRanking(cg1);
        assertTrue( cg1.getRank().intValue() > cg2.getRank().intValue() && 
                cg1.getRank().intValue() > cg3.getRank().intValue());
        assertTrue( cg2.getRank().intValue() < cg1.getRank().intValue() && 
                cg2.getRank().intValue() < cg3.getRank().intValue());
        
        
        cgl.removeCandidate(cg1);
        cgl.removeCandidate(cg2);
        cgl.removeCandidate(cg3);
        
        assertTrue( cgl.getCandidates().size()==0 );
    }
}
