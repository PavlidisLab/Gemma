
package edu.columbia.gemma.genome.gene;

import java.util.Collection;

import java.util.Iterator;
import net.sf.hibernate.SessionFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.BaseDAOTestCase;
import edu.columbia.gemma.genome.GeneDao;
import edu.columbia.gemma.genome.Gene;
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
public class CandidateGeneListDAOImplTest extends BaseDAOTestCase {
	 private CandidateGeneListDao daoCGL = null;
	 private TaxonDao daoTaxon = null;
	 private GeneDao daoGene = null;
	 private Gene g = null;
	 private Gene g2 = null;
	 private Taxon t = null;
	 private CandidateGeneList cgl = null;
	 private final Log log = LogFactory.getLog( CandidateGeneListDAOImplTest.class );
	 private SessionFactory sf = null;
 
	 protected void setUp() throws Exception {
		 super.setUp();

		 sf = ( SessionFactory ) ctx.getBean( "sessionFactory" );
		 daoCGL = ( CandidateGeneListDao ) ctx.getBean( "candidateGeneListDao" );
		 daoGene = (GeneDao) ctx.getBean("geneDao");
		 daoTaxon = (TaxonDao) ctx.getBean("taxonDao");

		 cgl = CandidateGeneList.Factory.newInstance();
	        
		 t = daoTaxon.findByCommonName("mouse");
		 if(t==null){
			 t = Taxon.Factory.newInstance();
			 t.setCommonName("mouse");
			 t.setScientificName("mouse");
			 daoTaxon.create(t);
		 }	
		 Collection col = daoGene.findByOfficalSymbol("foo");
		 if( col.size()>0 )
			 g = (Gene) col.iterator().next();
		 else{
			 g = Gene.Factory.newInstance();
			 g.setName("testmygene");
			 g.setOfficialSymbol("foo");
			 g.setOfficialName("testmygene");
			 g.setTaxon(t);
			 daoGene.create(g);
		 }	
		 col = daoGene.findByOfficalSymbol("foo2");
		 if( col.size()>0 )
			 g2 = (Gene) col.iterator().next();
		 else{
			 g2 = Gene.Factory.newInstance();
			 g2.setName("testmygene2");
			 g2.setOfficialSymbol("foo2");
			 g2.setOfficialName("testmygene2");
			 g2.setTaxon(t);
			 daoGene.create(g2);
		 }
		 
		 daoCGL.create(cgl);
	 }
	 
	 public final void testAddGeneToList() throws Exception {
		 CandidateGene cg = cgl.addCandidate(g);
		 daoCGL.update(cgl);
		 assertEquals(cgl.getCandidates().size(), 1);
		 assertEquals(cg.getGene().getName(), "testmygene" ); 
		 cgl.removeCandidate(cg);
	 }
	 
	 public final void testRemoveGeneFromList() throws Exception {
		 CandidateGene cg = cgl.addCandidate(g2);
		 daoCGL.update(cgl);
		 cgl.removeCandidate(cg);
		 daoCGL.update(cgl);
		 assert(cgl.getCandidates().size()==0);
	 }
	 
	 public final void testRankingChanges() throws Exception {
		 CandidateGene cg1 = cgl.addCandidate(g);
		 CandidateGene cg2 = cgl.addCandidate(g2);
		 cgl.increaseRanking(cg2);
		 daoCGL.update(cgl);
		 Collection c = cgl.getCandidates();
		 for(Iterator iter=c.iterator();iter.hasNext();){
			 cg1 = (CandidateGene)iter.next();
			 System.out.println(cg1.getName());
			 if(cg1.getGene().getName().matches("testmygene2"))
				 assertEquals((int)cg1.getRank(),(int)0);
			 else
				 assertEquals((int)cg1.getRank(),(int)1);
		 }
	 }
	 
	 protected void tearDown() throws Exception {
		 daoCGL.remove(cgl);
		 daoCGL = null;
		 if( g != null )
			 daoGene.remove(g);
		 daoGene=null;
		 daoTaxon=null;	
	 }
}
