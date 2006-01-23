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
package edu.columbia.gemma.expression.experiment;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import java.util.Collection;
import java.util.HashSet;

import junit.framework.TestCase;
import edu.columbia.gemma.common.auditAndSecurity.Person;

/**
 * @author daq2101
 * @version $Id$
 */
public class ExpressionExperimentServiceImplTest extends TestCase {
    Collection<ExpressionExperiment> c;
    Collection<ExpressionExperiment> cJustTwelve;
    private ExpressionExperiment ee = null;
    private Person nobody = null;
    private Person admin = null;
    private ExpressionExperimentServiceImpl svc = null;

    private ExpressionExperimentDao eeDao;

    @SuppressWarnings("unchecked")
    protected void setUp() throws Exception {
        super.setUp();

        svc = new ExpressionExperimentServiceImpl();

        eeDao = createMock( ExpressionExperimentDao.class );
        svc.setExpressionExperimentDao( eeDao );

        nobody = Person.Factory.newInstance();
        admin = Person.Factory.newInstance();

        ee = ExpressionExperiment.Factory.newInstance();
        ee.setDescription( "From test" );
        ee.setName( "Test experiment" );
        ee.setOwner( nobody );

        ee.getInvestigators().add( admin );
        ee = svc.findOrCreate( ee );

        c = new HashSet<ExpressionExperiment>();
        ExpressionExperiment numberTwelve = ExpressionExperiment.Factory.newInstance();
        numberTwelve.setId( new Long( 12 ) );

        c.add( numberTwelve );
        c.add( ExpressionExperiment.Factory.newInstance() );
        c.add( ExpressionExperiment.Factory.newInstance() );

        cJustTwelve = new HashSet<ExpressionExperiment>();
        cJustTwelve.add( numberTwelve );

    }

    public void testExpressionExperimentFindByInvestigator() {

        reset( eeDao );
        eeDao.findByInvestigator( nobody );
        expectLastCall().andReturn( cJustTwelve );
        replay( eeDao );
        svc.findByInvestigator( nobody );
        verify( eeDao );
    }

    public void testExpressionExperimentFindAll() {
        reset( eeDao );
        eeDao.loadAll();
        expectLastCall().andReturn( c );
        replay( eeDao );
        svc.getAllExpressionExperiments();
        verify( eeDao );
    }

    // public void testExpressionExperimentFindById() {
    // reset( eeDao );
    // eeDao.findById( 13 );
    // expectLastCall().andReturn( null );
    // replay( eeDao );
    // svc.findById( 13 );
    // verify( eeDao );
    // }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
}