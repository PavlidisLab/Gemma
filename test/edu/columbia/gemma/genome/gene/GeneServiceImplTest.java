/*
 * The Gemma project.
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.genome.gene;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.BaseDAOTestCase;
import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.GeneDao;
import edu.columbia.gemma.genome.Taxon;
import edu.columbia.gemma.genome.TaxonDao;
import java.util.Collection;

/**
 * @see edu.columbia.gemma.genome.gene.GeneService
 */
public class GeneServiceImplTest extends BaseDAOTestCase {

	private final Log log = LogFactory.getLog(CandidateGeneImplTest.class);
	private Taxon t = null;
	private Gene g = null;
	private GeneDao gDAO = null;
	private TaxonDao tDAO=null;
	
	protected void setUp() throws Exception {		
		
		tDAO = (TaxonDao)ctx.getBean("taxonDao");
        t = Taxon.Factory.newInstance();
		t.setCommonName("moose");
		t.setScientificName("moose");
		tDAO.create(t);
		
		
	}
	public void testGeneServiceImpl(){
		Collection cON=null;
		Collection cOS=null;
		Collection cOSI=null;
		Collection cAll=null;
		GeneService svc = (GeneService)ctx.getBean("geneService");
	    
		g = Gene.Factory.newInstance();
		g.setName("rabble");
		g.setOfficialSymbol("rab");
		g.setOfficialName("rabblebong");
		g.setTaxon(t);
		g = svc.createGene(g);
		g.setOfficialName("rabble");
		g = svc.updateGene(g);
		
		cON = svc.findByOfficialName("rabble");
	    cOS = svc.findByOfficialSymbol("rab");
	    cOSI = svc.findByOfficialSymbolInexact("ra%");
	    long geneID = g.getId().longValue();
	    Gene gLookup = svc.findByID(geneID);
	    cAll = svc.getAllGenes();
		
		assertTrue( cON != null && cON.size()==1);
		assertTrue( cOS != null && cOS.size()==1);
		assertTrue( cOSI != null && cOSI.size()>0);
		assertTrue( cAll != null && cAll.size()>0);
		assertTrue( gLookup != null && gLookup.getId().longValue() == geneID);
		
		svc.removeGene("rabble");
	}
	
	protected void tearDown() throws Exception{
		tDAO.remove(t);
	}
}
