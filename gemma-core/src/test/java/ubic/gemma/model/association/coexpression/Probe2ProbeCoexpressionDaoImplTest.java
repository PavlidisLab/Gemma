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
package ubic.gemma.model.association.coexpression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class Probe2ProbeCoexpressionDaoImplTest extends BaseSpringContextTest {

    ExpressionExperiment ee;
    ExpressionExperimentService ees;

    Probe2ProbeCoexpressionService ppcs;

    @SuppressWarnings("unchecked")
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        this.endTransaction();
        ee = this.getTestPersistentCompleteExpressionExperiment( false );
        ees = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );

        ees.thaw( ee );

        Collection<QuantitationType> qts = ees.getQuantitationTypes( ee );

        // this is bogus, it should represent "pearson correlation" for example.
        QuantitationType qt = qts.iterator().next();

        ppcs = ( Probe2ProbeCoexpressionService ) this.getBean( "probe2ProbeCoexpressionService" );

        Collection<DesignElementDataVector> dvs = ee.getDesignElementDataVectors();

        List<DesignElementDataVector> dvl = new ArrayList<DesignElementDataVector>( dvs );

        int j = dvs.size();

        for ( int i = 0; i < j - 1; i += 2 ) {
            Probe2ProbeCoexpression ppc = MouseProbeCoExpression.Factory.newInstance();
            ppc.setFirstVector( dvl.get( i ) );
            ppc.setSecondVector( dvl.get( i + 1 ) );
            ppc.setMetric( qt );
            ppc.setScore( 0.0 );
            ppc.setPvalue( 0.2 );
            ppcs.create( ppc );
        }

        this.flushAndClearSession();

    }

    @Override
    protected void onTearDownInTransaction() throws Exception {
        super.onTearDownInTransaction();
        if ( ee != null ) {
            ees.delete( ee );
        }
    }

    public void testCountLinks() {
        Integer countLinks = ppcs.countLinks( ee );
        assertEquals( 6, countLinks.intValue() );
    }

    /**
     * 
     *
     */
    public void testHandleDeleteLinksExpressionExperiment() {
        ppcs.deleteLinks( ee );
        // no fail condition..
    }

}
