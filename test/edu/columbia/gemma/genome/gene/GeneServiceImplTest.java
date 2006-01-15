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
package edu.columbia.gemma.genome.gene;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import java.util.Collection;
import java.util.HashSet;

import junit.framework.TestCase;
import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.GeneDao;
import edu.columbia.gemma.genome.QtlDao;
import edu.columbia.gemma.genome.Taxon;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2006 University of British Columbia
 * 
 * @author daq2101
 * @version $Id$
 */
public class GeneServiceImplTest extends TestCase {

    private Taxon t = null;
    private Gene g = null;
    private GeneDao geneDaoMock;
    private QtlDao qtlDaoMock;
    GeneServiceImpl svc;
    Collection<Gene> allThree = new HashSet<Gene>();
    Collection<Gene> justRab = new HashSet<Gene>();
    Collection<Gene> justRabble = new HashSet<Gene>();

    protected void setUp() throws Exception {
        geneDaoMock = createMock( GeneDao.class );
        qtlDaoMock = createMock( QtlDao.class );
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

        g = Gene.Factory.newInstance();
        g.setOfficialName( "rabblebong" );
        g.setTaxon( t );
        allThree.add( g );
        g = Gene.Factory.newInstance();
        g.setOfficialName( "rabble" );
        allThree.add( g );
        justRabble.add( g );

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
        svc.getAllGenes();
        verify( geneDaoMock );
    }

    protected void tearDown() throws Exception {
        justRab.clear();
        justRabble.clear();
        allThree.clear();
    }
}
