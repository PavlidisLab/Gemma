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
        
        CandidateGeneListServiceImpl cglService = new CandidateGeneListServiceImpl();
        
        Gene gene1 = makeGene("foo1");
        Gene gene2 = makeGene("foo2");
        Gene gene3 = makeGene("foo3");
        
        // add some new genes as candidategenes (using makeGene shortcut)
        CandidateGene cg1 = cglService.handleAddCandidateToList(cgl,gene1);
        CandidateGene cg2 = cglService.handleAddCandidateToList(cgl,gene2);
        CandidateGene cg3 = cglService.handleAddCandidateToList(cgl,gene3);
        
        cglService.handleRemoveCandidateFromList(cgl, cg1);
        cglService.handleRemoveCandidateFromList(cgl, cg2);
        cglService.handleRemoveCandidateFromList(cgl, cg3);
        daoGene.remove(gene1);
        daoGene.remove(gene2);
        daoGene.remove(gene3);
    }
}
