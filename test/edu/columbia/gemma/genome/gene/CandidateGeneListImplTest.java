package edu.columbia.gemma.genome.gene;


import java.util.ArrayList;
import java.util.Collection;

import edu.columbia.gemma.BaseDAOTestCase;
import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.GeneDao;
import edu.columbia.gemma.genome.Taxon;
import edu.columbia.gemma.genome.TaxonDao;

/**
 *
 *
 * <hr>
 * <p>Copyright (c) 2004, 2005 Columbia University
 * @author daq2101
 * @version $Id$
 */ 
public class CandidateGeneListImplTest extends BaseDAOTestCase {

    private TaxonDao daoTaxon = null;
    private Taxon t = null;
    private GeneDao daoGene = null;
    private CandidateGeneList cgl = null;
    private CandidateGeneListDao daoCGL = null;
    private CandidateGeneDao daoCG = null;
    ArrayList dummygenes = null;
    
    private Gene makeGene(String officialName ){
        
        Gene g = null;
        Collection c = daoGene.findByOfficalSymbol(officialName);
		if( c.isEmpty() ){
		    g = Gene.Factory.newInstance();
			g.setName(officialName);
			g.setOfficialSymbol(officialName);
			g.setOfficialName(officialName);
			g.setTaxon(t);
			daoGene.create(g);
		}
		else
		    g = (Gene)c.iterator().next();
		
		return g;	
    }
    protected void setUp() throws Exception {
        super.setUp();
        // create dummy taxon for Mouse
		daoTaxon = (TaxonDao) ctx.getBean("taxonDao");
		daoGene = (GeneDao) ctx.getBean("geneDao");
		
		t = daoTaxon.findByCommonName("mouse");
		if( t==null){
		    t = Taxon.Factory.newInstance();
		    t.setCommonName("mouse");
			t.setScientificName("mouse");
			daoTaxon.create(t);
		}
		
		// create new list for fun
		daoCGL = (CandidateGeneListDao) ctx.getBean("candidateGeneListDao");
		daoCG = (CandidateGeneDao) ctx.getBean("candidateGeneDao");
		cgl = CandidateGeneList.Factory.newInstance();
		cgl.setDescription("Test my candidate list");
		cgl.setName("Test Candidates");
	    daoCGL.create(cgl);
    }
    
    protected void tearDown() throws Exception {
        super.tearDown();
        daoGene = null;
		daoTaxon.remove(t);
		daoTaxon=null;
		daoCGL=null;
    }

    public void testUseCandidateGeneList() {
        
        CandidateGeneListService cglService = (CandidateGeneListService) ctx.getBean("candidateGeneListService");
        
        Gene gene1 = makeGene("foo1");
        Gene gene2 = makeGene("foo2");
        Gene gene3 = makeGene("foo3");
        /*
        CandidateGene cg1 = CandidateGene.Factory.newInstance();
        cg1.setGene(gene1);
        CandidateGene cg2 = CandidateGene.Factory.newInstance();
        cg2.setGene(gene2);
        CandidateGene cg3 = CandidateGene.Factory.newInstance();
        cg3.setGene(gene3);
        daoCG.create(cg1);
        daoCG.create(cg2);
        daoCG.create(cg3);
        cgl.addCandidate(cg1);
        cgl.addCandidate(cg2);
        cgl.addCandidate(cg3);
        daoCGL.update(cgl);
        
        cgl.removeCandidate(cg3);
        cgl.removeCandidate(cg2);
        cgl.removeCandidate(cg1);
        
        daoCG.remove(cg1);
        daoCG.remove(cg2);
        daoCG.remove(cg3);
        
        daoGene.remove(gene1);
        daoGene.remove(gene2);
        daoGene.remove(gene3);
        */
        }
}
