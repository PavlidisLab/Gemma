package edu.columbia.gemma.genome.gene;

import edu.columbia.gemma.BaseDAOTestCase;
import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.Taxon;
import edu.columbia.gemma.genome.gene.CandidateGene;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <hr>
 * <p>
 * Copyright (c) 2005 Columbia University
 * @author David Quigley
 */
public class CandidateGeneImplTest extends BaseDAOTestCase{

    private final Log log = LogFactory.getLog(CandidateGeneImplTest.class);
    
	public void testSetCandidateGene() {
	  
	    // SET UP
        Gene g = null;
        Taxon t = null;
        
        t = Taxon.Factory.newInstance();
		t.setCommonName("mouse");
		t.setScientificName("mouse");
		
		g = Gene.Factory.newInstance();
		g.setName("testmygene");
		g.setOfficialSymbol("foo");
		g.setOfficialName("testmygene");
		g.setTaxon(t);
		
		// TEST
	    CandidateGene cg = CandidateGene.Factory.newInstance();
	    cg.setRank(new Integer(1));
	    cg.setGene(g);
	    
	    assertTrue(cg.getGene().equals(g));
	}

}
