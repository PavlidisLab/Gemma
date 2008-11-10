/*
 * The Gemma project.
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
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneDao;
import ubic.gemma.model.genome.BaseQtlDao;
import ubic.gemma.model.genome.Taxon;

/**
 * @author daq2101
 * @version $Id$
 */
public class GeneServiceImplTest extends TestCase {

    private Taxon t = null;
    private Gene g = null;
    private Gene g2 = null;
    private Gene g3 = null;
    private GeneDao geneDaoMock;
    private BaseQtlDao qtlDaoMock;
    GeneServiceImpl svc;
    Collection<Gene> allThree = new HashSet<Gene>();
    Collection<Gene> justRab = new HashSet<Gene>();
    Collection<Gene> justRabble = new HashSet<Gene>();

    @Override
    protected void setUp() throws Exception {
        geneDaoMock = createMock( GeneDao.class );
        qtlDaoMock = createMock( BaseQtlDao.class );
        svc = new GeneServiceImpl();
        svc.setGeneDao( geneDaoMock );
        svc.setQtlDao( qtlDaoMock );
        t = Taxon.Factory.newInstance();
        t.setCommonName( "moose" );
        t.setScientificName( "moose" );
        // tDAO.create( t );

        g = Gene.Factory.newInstance();
        g.setOfficialName( "rabble" );
        g.setOfficialSymbol( "rab" );
        allThree.add( g );
        justRab.add( g );

        g2 = Gene.Factory.newInstance();
        g2.setOfficialName( "rabblebong" );
        g2.setTaxon( t );
        allThree.add( g2 );

        g3 = Gene.Factory.newInstance();
        g3.setOfficialName( "rabble" );
        g3.setNcbiId( "12345" );
        allThree.add( g3 );
        justRabble.add( g3 );

    }

    public void testFindByAccessionNoSource() {
        reset( geneDaoMock );
        geneDaoMock.findByAccession( "12345", null );
        expectLastCall().andReturn( g3 );
        replay( geneDaoMock );
        svc.findByAccession( "12345", null );
        verify( geneDaoMock );
    }

    public void testFindByNcbiId() {
        reset( geneDaoMock );
        geneDaoMock.findByNcbiId( "12345" );
        expectLastCall().andReturn( justRabble );
        replay( geneDaoMock );
        svc.findByNCBIId( "12345" );
        verify( geneDaoMock );
    }

    public void testFindByOfficialName() {
        reset( geneDaoMock );
        geneDaoMock.findByOfficialName( "rabble" );
        expectLastCall().andReturn( justRab );
        replay( geneDaoMock );
        svc.findByOfficialName( "rabble" );
        verify( geneDaoMock );
    }

    public void testFindByOfficialSymbol() {
        reset( geneDaoMock );
        geneDaoMock.findByOfficalSymbol( "rabble" );
        expectLastCall().andReturn( justRab );
        replay( geneDaoMock );
        svc.findByOfficialSymbol( "rabble" );
        verify( geneDaoMock );
    }

    public void testFindByOfficialSymbolInexact() {
        reset( geneDaoMock );
        geneDaoMock.findByOfficialSymbolInexact( "ra%" );
        expectLastCall().andReturn( allThree );
        replay( geneDaoMock );
        svc.findByOfficialSymbolInexact( "ra%" );
        verify( geneDaoMock );
    }

    public void testFindAll() {
        reset( geneDaoMock );
        geneDaoMock.loadAll();
        expectLastCall().andReturn( allThree );
        replay( geneDaoMock );
        svc.loadAll();
        verify( geneDaoMock );
    }

    @Override
    protected void tearDown() throws Exception {
        justRab.clear();
        justRabble.clear();
        allThree.clear();
    }
}
