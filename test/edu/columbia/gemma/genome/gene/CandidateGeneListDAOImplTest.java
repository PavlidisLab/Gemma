/*
 * The Gemma project
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

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.BaseDAOTestCase;
import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.GeneDao;
import edu.columbia.gemma.genome.Taxon;
import edu.columbia.gemma.genome.TaxonDao;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004, 2005 Columbia University
 * 
 * @author daq2101
 * @version $Id$
 */
public class CandidateGeneListDAOImplTest extends BaseDAOTestCase {
    private final Log log = LogFactory.getLog( CandidateGeneListDAOImplTest.class );

    private CandidateGeneListDao daoCGL = null;
    private TaxonDao daoTaxon = null;
    private GeneDao daoGene = null;
    private Gene g = null;
    private Gene g2 = null;
    private Taxon t = null;
    private CandidateGeneList cgl = null;

    protected void setUp() throws Exception {
        super.setUp();

        daoCGL = ( CandidateGeneListDao ) ctx.getBean( "candidateGeneListDao" );
        daoGene = ( GeneDao ) ctx.getBean( "geneDao" );
        daoTaxon = ( TaxonDao ) ctx.getBean( "taxonDao" );

        cgl = CandidateGeneList.Factory.newInstance();

        t = Taxon.Factory.newInstance();
        t.setCommonName( "mouse" );
        t.setScientificName( "Mus musculus" );
        t = daoTaxon.findOrCreate( t );

        g = Gene.Factory.newInstance();
        g.setName( "testmygene" );
        g.setOfficialSymbol( "foo" );
        g.setOfficialName( "testmygene" );
        g.setTaxon( t );
        daoGene.findOrCreate( g );

        g2 = Gene.Factory.newInstance();
        g2.setName( "testmygene2" );
        g2.setOfficialSymbol( "foo2" );
        g2.setOfficialName( "testmygene2" );
        g2.setTaxon( t );
        daoGene.findOrCreate( g2 );

        daoCGL.create( cgl );
    }

    public final void testAddGeneToList() throws Exception {
        log.info( "testing adding gene to list" );
        CandidateGene cg = cgl.addCandidate( g );
        assertEquals( cgl.getCandidates().size(), 1 );
        assertEquals( cg.getGene().getName(), "testmygene" );
    }

    public final void testRemoveGeneFromList() throws Exception {
        log.info( "testing removing gene from list" );
        CandidateGene cg = cgl.addCandidate( g2 );
        log.info( cgl.getCandidates().size() + " candidates to start" );
        cg = ( CandidateGene ) cgl.getCandidates().iterator().next(); // get the persistent object.
        log.info( cgl.getCandidates().size() + " candidates just before deleting" );
        cgl.removeCandidate( cg );
        log.info( cgl.getCandidates().size() + " candidates left" );
        assert ( cgl.getCandidates().size() == 0 );
    }

    public final void testRankingChanges() throws Exception {
        log.info( "testing ranking changes" );
        CandidateGene cg1 = cgl.addCandidate( g );
        CandidateGene cg2 = cgl.addCandidate( g2 );
        cgl.increaseRanking( cg2 );
        Collection c = cgl.getCandidates();
        for ( Iterator iter = c.iterator(); iter.hasNext(); ) {
            cg1 = ( CandidateGene ) iter.next();
            if ( cg1.getGene().getName().matches( "testmygene2" ) )
                assertEquals( cg1.getRank().intValue(), 0 );
            else
                assertEquals( cg1.getRank().intValue(), 1 );
        }
    }

    protected void tearDown() throws Exception {
        daoCGL.remove( cgl );
        daoCGL = null;
        if ( g != null ) daoGene.remove( g );
        daoGene = null;
        daoTaxon = null;
    }
}
