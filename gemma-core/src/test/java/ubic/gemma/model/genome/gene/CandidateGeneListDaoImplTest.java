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

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.Person;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author daq2101
 * @version $Id$
 */
public class CandidateGeneListDaoImplTest extends BaseSpringContextTest {
    private final Log log = LogFactory.getLog( CandidateGeneListDaoImplTest.class );

    private CandidateGeneListDao candidateGeneListDao;
    private PersisterHelper persisterHelper;
    private Gene g;
    private Gene g2;
    private Taxon t;
    private CandidateGeneList candidateGeneList;

    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        persisterHelper = ( PersisterHelper ) this.getBean( "persisterHelper" );

        candidateGeneList = CandidateGeneList.Factory.newInstance();

        AuditTrail ad = AuditTrail.Factory.newInstance();
//        User u = User.Factory.newInstance();
//        u.setUserName( J"joe" );
//        ad.set
        
        ad = ( AuditTrail ) persisterHelper.persist( ad );
        candidateGeneList.setAuditTrail( ad );

        // have to use a real existing taxon (PP)
        t = Taxon.Factory.newInstance();
        t.setCommonName( "mouse" );
        t.setScientificName( "Mus musculus" );
        t = ( Taxon ) persisterHelper.persist( t );

        ad = AuditTrail.Factory.newInstance();

        g = Gene.Factory.newInstance();
        g.setName( "testmygene" );
        g.setOfficialSymbol( "foo" );
        g.setOfficialName( "testmygene" );
        g.setTaxon( t );
        ad = AuditTrail.Factory.newInstance();
        ad = ( AuditTrail ) persisterHelper.persist( ad );
        g.setAuditTrail( ad );
        g = ( Gene ) persisterHelper.persist( g );

        g2 = Gene.Factory.newInstance();
        g2.setName( "testmygene2" );
        g2.setOfficialSymbol( "foo2" );
        g2.setOfficialName( "testmygene2" );
        g2.setTaxon( t );
        ad = AuditTrail.Factory.newInstance();
        ad = ( AuditTrail ) persisterHelper.persist( ad );
        g2.setAuditTrail( ad );
        g2 = ( Gene ) persisterHelper.persist( g2 );

        ad = AuditTrail.Factory.newInstance();
        ad = ( AuditTrail ) persisterHelper.persist( ad );

        candidateGeneList.setAuditTrail( ad );

        Person u = Person.Factory.newInstance();
        u.setName( "Joe Blow" );
        u = ( Person ) persisterHelper.persist( u );

        candidateGeneList.setOwner( u );

        candidateGeneList = ( CandidateGeneList ) candidateGeneListDao.create( candidateGeneList );

    }

    public final void testAddGeneToList() throws Exception {
        log.info( "testing adding gene to list" );
        assert candidateGeneList != null;
        CandidateGene cg = candidateGeneList.addCandidate( g );
        assertEquals( candidateGeneList.getCandidates().size(), 1 );
        assertEquals( cg.getGene().getName(), "testmygene" );
    }

    public final void testRemoveGeneFromList() throws Exception {
        log.info( "testing removing gene from list" );
        assert candidateGeneList != null;
        CandidateGene cg = candidateGeneList.addCandidate( g2 );
        log.info( candidateGeneList.getCandidates().size() + " candidates to start" );
        cg = candidateGeneList.getCandidates().iterator().next(); // get the persistent object.
        log.info( candidateGeneList.getCandidates().size() + " candidates just before deleting" );
        candidateGeneList.removeCandidate( cg );
        log.info( candidateGeneList.getCandidates().size() + " candidates left" );
        assert ( candidateGeneList.getCandidates().size() == 0 );
    }

    public final void testRankingChanges() throws Exception {
        log.info( "testing ranking changes" );
        assert candidateGeneList != null;
        CandidateGene cg1 = candidateGeneList.addCandidate( g );
        CandidateGene cg2 = candidateGeneList.addCandidate( g2 );
        candidateGeneList.increaseRanking( cg2 );
        Collection c = candidateGeneList.getCandidates();
        for ( Iterator iter = c.iterator(); iter.hasNext(); ) {
            cg1 = ( CandidateGene ) iter.next();
            if ( cg1.getGene().getName().matches( "testmygene2" ) )
                assertEquals( cg1.getRank().intValue(), 0 );
            else
                assertEquals( cg1.getRank().intValue(), 1 );
        }
    }

    /**
     * @param candidateGeneListDao The candidateGeneListDao to set.
     */
    public void setCandidateGeneListDao( CandidateGeneListDao candidateGeneListDao ) {
        this.candidateGeneListDao = candidateGeneListDao;
    }

}
