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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ubic.gemma.core.testing.BaseSpringContextTest;
import ubic.gemma.model.common.auditAndSecurity.Person;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDaoImpl;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentServiceImpl;

import java.util.Collection;
import java.util.HashSet;

import static org.easymock.EasyMock.*;

/**
 * @author daq2101
 * @author paul
 */
public class ExpressionExperimentServiceImplTest extends BaseSpringContextTest {
    private Collection<ExpressionExperiment> c;
    private Collection<ExpressionExperiment> cJustTwelve;
    private ExpressionExperiment ee = null;
    private Person nobody = null;

    private ExpressionExperimentService svc;
    private ExpressionExperimentDao eeDao;

    @SuppressWarnings("Duplicates") // Not effective to extract
    @Test
    public void testExpressionExperimentFindAll() {
        reset( eeDao );
        eeDao.loadAll();
        expectLastCall().andReturn( c );
        replay( eeDao );
        svc.loadAll();
        verify( eeDao );
    }

    @Before
    public void setUp() {

        eeDao = createMock( ExpressionExperimentDaoImpl.class );
        svc = new ExpressionExperimentServiceImpl( eeDao );

        nobody = Person.Factory.newInstance();
        Person admin = Person.Factory.newInstance();

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

        c = new HashSet<>();
        ExpressionExperiment numberTwelve = ExpressionExperiment.Factory.newInstance();
        numberTwelve.setId( 12L );

        c.add( numberTwelve );
        c.add( ExpressionExperiment.Factory.newInstance() );
        c.add( ExpressionExperiment.Factory.newInstance() );

        cJustTwelve = new HashSet<>();
        cJustTwelve.add( numberTwelve );

    }

    @After
    public void tearDown() {
        ee = null;
    }
}