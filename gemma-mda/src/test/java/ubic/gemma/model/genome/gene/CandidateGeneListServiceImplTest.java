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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import java.util.Collection;
import java.util.HashSet;

import junit.framework.TestCase;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserDao;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneDao;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonDao;

/**
 * @author daq2101
 * @author pavlidis
 * @version $Id$
 */
public class CandidateGeneListServiceImplTest extends TestCase {

    private CandidateGeneDao candidateGeneDaoMock;
    private CandidateGeneListDao candidateGeneListDaoMock;
    private Gene g, g2, g3, g4 = null;

    private GeneDao geneDaoMock;
    private User p = null;
    private UserDao UserDaoMock;
    private CandidateGeneListServiceImpl svc;
    private Taxon t = null;

    private TaxonDao taxonDaoMock;

    public void testCandidateGeneListServiceCreateByName() throws Exception {
        reset( candidateGeneListDaoMock );
        // test create CandidateGeneList
        CandidateGeneList cgl = CandidateGeneList.Factory.newInstance();
        cgl.setName( "New Candidate List from service test" );

        candidateGeneListDaoMock.create( cgl );
        expectLastCall().andReturn( cgl );

        replay( candidateGeneListDaoMock );
        svc.saveCandidateGeneList( cgl );
        // svc.createByName( "New Candidate List from service test" );
        verify( candidateGeneListDaoMock );

    }

    public void testCandidateGeneListServiceImplB() throws Exception {
        reset( candidateGeneListDaoMock );
        CandidateGeneList cgl = this.createTestList();

        candidateGeneListDaoMock.findByID( 190409L );
        expectLastCall().andReturn( cgl );
        replay( candidateGeneListDaoMock );
        svc.findByID( 190409L );
        verify( candidateGeneListDaoMock );

    }

    public void testCandidateGeneListServiceImplFindByContributer() throws Exception {
        reset( candidateGeneListDaoMock );
        CandidateGeneList cgl = CandidateGeneList.Factory.newInstance();
        Collection<CandidateGeneList> cByName = new HashSet<CandidateGeneList>();
        cByName.add( cgl );
        candidateGeneListDaoMock.findByContributer( p );
        expectLastCall().andReturn( cByName );
        replay( candidateGeneListDaoMock );
        svc.findByContributer( p );
        verify( candidateGeneListDaoMock );
    }

    // Fragments of other test I haven't bother to mock.
    //        
    // Collection cAll = svc.getAll();
    //
    // log.debug( "All: " + cAll.size() + " candidate lists." );
    // Long cgl_id = cgl.getId();
    //
    // cgl = svc.findByID( cgl_id.longValue() );
    //
    // assertTrue( cByContributer != null && cByContributer.size() >= 1 );
    // assertTrue( cAll != null && cAll.size() >= 1 );
    //
    // // test remove CandidateGeneList
    //
    // svc.removeCandidateGeneList( cgl );
    //
    // cgl = svc.findByID( cgl_id.longValue() );
    // assertNull( cgl );

    public void testCandidateGeneListServiceImplFindByGeneOfficialName() throws Exception {
        reset( candidateGeneListDaoMock );
        CandidateGeneList cgl = CandidateGeneList.Factory.newInstance();
        Collection<CandidateGeneList> cByName = new HashSet<CandidateGeneList>();
        cByName.add( cgl );

        candidateGeneListDaoMock.findByGeneOfficialName( "test gene two" );
        expectLastCall().andReturn( cByName );
        replay( candidateGeneListDaoMock );
        svc.findByGeneOfficialName( "test gene two" );
        verify( candidateGeneListDaoMock );
    }

    private CandidateGeneList createTestList() {
        CandidateGeneList cgl = CandidateGeneList.Factory.newInstance();
        cgl.setId( new Long( 190409 ) );

        CandidateGene cg = cgl.addCandidate( g );
        cg.setAuditTrail( AuditTrail.Factory.newInstance() );
        cg.getAuditTrail().start( "Created CandidateGene", p );

        CandidateGene cg2 = cgl.addCandidate( g2 );
        cg2.setAuditTrail( AuditTrail.Factory.newInstance() );
        cg2.getAuditTrail().start( "Created CandidateGene", p );

        CandidateGene cg3 = cgl.addCandidate( g3 );
        cg3.setAuditTrail( AuditTrail.Factory.newInstance() );
        cg3.getAuditTrail().start( "Created CandidateGene", p );

        CandidateGene cg4 = cgl.addCandidate( g4 );
        cg4.setAuditTrail( AuditTrail.Factory.newInstance() );
        cg4.getAuditTrail().start( "Created CandidateGene", p );

        cgl.setName( "New CL Test" );
        return cgl;
    }

    @Override
    protected void setUp() throws Exception {

        UserDaoMock = createMock( UserDao.class );
        taxonDaoMock = createMock( TaxonDao.class );
        geneDaoMock = createMock( GeneDao.class );
        candidateGeneDaoMock = createMock( CandidateGeneDao.class );
        candidateGeneListDaoMock = createMock( CandidateGeneListDao.class );

        t = Taxon.Factory.newInstance();
        t.setScientificName( "test testicus" );
        t.setCommonName( "common test" );
        taxonDaoMock.create( t );

        g = Gene.Factory.newInstance();
        g.setName( "test gene one" );
        g.setOfficialName( "test gene one" );
        g.setTaxon( t );

        g2 = Gene.Factory.newInstance();
        g2.setName( "test gene two" );
        g2.setOfficialName( "test gene two" );
        g2.setTaxon( t );

        g3 = Gene.Factory.newInstance();
        g3.setName( "test gene three" );
        g3.setOfficialName( "test gene three" );
        g3.setTaxon( t );

        g4 = Gene.Factory.newInstance();
        g4.setName( "test gene four" );
        g4.setOfficialName( "test gene four" );
        g4.setTaxon( t );

        p = User.Factory.newInstance();

        p.setName( "David" );
        p.setLastName( "Quigley" );
        p.setEmail( "daq2101@columbia.edu" );
        UserDaoMock.create( p );

        svc = new CandidateGeneListServiceImpl();
        svc.setCandidateGeneDao( this.candidateGeneDaoMock );
        svc.setCandidateGeneListDao( this.candidateGeneListDaoMock );
        svc.setGeneDao( this.geneDaoMock );

        svc.setActor( p );

    }

    @Override
    protected void tearDown() throws Exception {

    }
}