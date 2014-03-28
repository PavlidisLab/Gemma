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
package ubic.gemma.model.expression.experiment;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import java.util.Collection;
import java.util.HashSet;

import junit.framework.TestCase;
import ubic.gemma.expression.experiment.service.ExpressionExperimentServiceImpl;
import ubic.gemma.model.common.auditAndSecurity.Person;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;

/**
 * @author daq2101
 * @author paul
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

    public void testExpressionExperimentFindAll() {
        reset( eeDao );
        eeDao.loadAll();
        expectLastCall().andReturn( c );
        replay( eeDao );
        svc.loadAll();
        verify( eeDao );
    }

    public void testExpressionExperimentFindByInvestigator() {
        reset( eeDao );
        eeDao.findByInvestigator( nobody );
        expectLastCall().andReturn( cJustTwelve );
        replay( eeDao );
        svc.findByInvestigator( nobody );
        verify( eeDao );
    }

    @Override
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

        ArrayDesign ad1 = ArrayDesign.Factory.newInstance();
        ad1.setShortName( "foo" );
        ArrayDesign ad2 = ArrayDesign.Factory.newInstance();
        ad2.setShortName( "bar" );

        for ( long i = 0; i < 10; i++ ) {
            BioAssay ba = BioAssay.Factory.newInstance();
            ba.setId( i + 1 );
            if ( i % 2 == 0 ) {
                ba.setArrayDesignUsed( ad1 );
            } else {
                ba.setArrayDesignUsed( ad2 );
            }
            ee.getBioAssays().add( ba );
        }

        ee.getInvestigators().add( admin );

        c = new HashSet<ExpressionExperiment>();
        ExpressionExperiment numberTwelve = ExpressionExperiment.Factory.newInstance();
        numberTwelve.setId( new Long( 12 ) );

        c.add( numberTwelve );
        c.add( ExpressionExperiment.Factory.newInstance() );
        c.add( ExpressionExperiment.Factory.newInstance() );

        cJustTwelve = new HashSet<ExpressionExperiment>();
        cJustTwelve.add( numberTwelve );

    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        ee = null;
    }
}