package edu.columbia.gemma.sequence.gene;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ListIterator;

import edu.columbia.gemma.BaseDAOTestCase;
import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.GeneDao;
import edu.columbia.gemma.genome.Taxon;
import edu.columbia.gemma.genome.TaxonDao;
import edu.columbia.gemma.genome.gene.CandidateGene;
import edu.columbia.gemma.genome.gene.CandidateGeneDao;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
/**
 * 
 *
 * <hr>
 * <p>Copyright (c) 2004 Columbia University
 * @author David
 * @version $Id$
 */
public class CandidateGeneListImplTest extends BaseDAOTestCase {

    private TaxonDao daoTaxon = null;
    private Taxon t = null;
    private GeneDao daoGene = null;
    private CandidateGeneDao daoCG = null;
    private CandidateGene cg = null;
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
        dummygenes = new ArrayList();
        // create dummy taxon for Mouse
		daoTaxon = (TaxonDao) ctx.getBean("taxonDao");
		daoGene = (GeneDao) ctx.getBean("geneDao");
		daoCG = (CandidateGeneDao) ctx.getBean("candidateGeneDao");
		t = daoTaxon.findByCommonName("mouse");
		if( t==null){
		    t = Taxon.Factory.newInstance();
		    t.setCommonName("mouse");
			t.setScientificName("mouse");
			daoTaxon.create(t);
		}
		
		// Create some dummy genes
		dummygenes.add( makeGene("foo1"));
		dummygenes.add( makeGene("foo2"));
		dummygenes.add( makeGene("foo3"));
		dummygenes.add( makeGene("foo4"));
    }
    
    protected void tearDown() throws Exception {
        super.tearDown();
        daoCG.remove(cg);
        ListIterator li = dummygenes.listIterator();
        while(li.hasNext()){
            daoGene.remove((Gene)li.next());
        }
		daoGene = null;
		daoTaxon.remove(t);
		daoTaxon=null;
    }

    public void testUseCandidateGeneList() {
        // Add first three dummygenes to the CandidateList in order.
        cg=CandidateGene.Factory.newInstance();
        
	    ListIterator li = dummygenes.listIterator();
	    cg.setGene((Gene)li.next());
	    cg.setRank(new Integer(1));
	    daoCG.create(cg);
	    
    }

}
