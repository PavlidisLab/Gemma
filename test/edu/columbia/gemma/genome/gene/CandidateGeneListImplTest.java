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
		cgl.setDescription("Test candidate list");
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
        
        ArrayList newcgs = new ArrayList();
        
        // add some new genes as candidategenes (using makeGene shortcut)
        newcgs.add(cglService.handleAddCandidateToList(cgl,makeGene("foo1")));
        newcgs.add(cglService.handleAddCandidateToList(cgl,makeGene("foo2")));
        newcgs.add(cglService.handleAddCandidateToList(cgl,makeGene("foo3")));
        
	    // now remove the genes from the list
	    java.util.Iterator iter = newcgs.iterator();
	    
	    CandidateGene cgKill = null;
	    Gene gKill = null;
	    while(iter.hasNext()){
	        cgKill = (CandidateGene) iter.next();
	        gKill = cgKill.getGene();
	        cglService.handleRemoveCandidateFromList(cgl, cgKill);
	        daoGene.remove(gKill);
	    }
        
    }

}
