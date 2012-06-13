/*
 * The Gemma project
 * 
 * Copyright (c) 2006-2010 University of British Columbia
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class Probe2ProbeCoexpressionServiceTest extends BaseSpringContextTest {

    ExpressionExperiment ee;

    @Autowired
    ExpressionExperimentService ees;

    Long firstProbeId;
    Long secondProbeId;

    @Autowired
    Probe2ProbeCoexpressionService ppcs;

    @Autowired
    ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;

    @Before
    public void setup() {

        ee = this.getTestPersistentCompleteExpressionExperiment( false );

        Collection<QuantitationType> qts = ees.getQuantitationTypes( ee );

        // this is bogus, it should represent "pearson correlation" for example, but doesn't matter for this test.
        QuantitationType qt = qts.iterator().next();

        Collection<ProcessedExpressionDataVector> dvs = processedExpressionDataVectorCreateService
                .computeProcessedExpressionData( ee );

        List<ProcessedExpressionDataVector> dvl = new ArrayList<ProcessedExpressionDataVector>( dvs );

        assertTrue( dvs.size() > 1 );

        ProbeCoexpressionAnalysis analysis = ProbeCoexpressionAnalysis.Factory.newInstance();
        analysis.setName( randomName() );
        analysis.setExperimentAnalyzed( ee );

        analysis = ( ProbeCoexpressionAnalysis ) this.persisterHelper.persist( analysis );

        this.firstProbeId = dvl.get( 0 ).getDesignElement().getId();
        this.secondProbeId = dvl.get( 1 ).getDesignElement().getId();

        assertNotNull( this.firstProbeId );

        Collection<Probe2ProbeCoexpression> coll = new HashSet<Probe2ProbeCoexpression>();

        for ( int i = 0, j = dvs.size(); i < j - 1; i += 2 ) {
            Probe2ProbeCoexpression ppc = MouseProbeCoExpression.Factory.newInstance();
            ppc.setFirstVector( dvl.get( i ) );
            ppc.setSecondVector( dvl.get( i + 1 ) );

            assertNotNull( ppc.getFirstVector().getDesignElement().getId() );

            ppc.setMetric( qt );
            ppc.setScore( 0.0 );
            ppc.setPvalue( 0.2 );
            ppc.setExpressionBioAssaySet( ee );
            ppc.setSourceAnalysis( analysis );
            coll.add( ppc );

        }

        ppcs.create( coll );
    }

    @Test
    public void testCountLinks() {
        Integer countLinks = ppcs.countLinks( ee );
        /*
         * This would be 6 but we divide the count by 2 as it is assumed we save each link twice.
         */
        assertEquals( 3, countLinks.intValue() );
    }

    /**
     */
    @Test
    public void testHandleDeleteLinksExpressionExperiment() {
        ppcs.deleteLinks( ee );
        Integer countLinks = ppcs.countLinks( ee );
        assertEquals( null, countLinks );
    }

    /**
     * @throws Exception
     */
    @Test
    public void testValidateProbesInCoexpression() throws Exception {
        Collection<Long> queryProbeIds = new ArrayList<Long>();
        queryProbeIds.add( this.firstProbeId );
        queryProbeIds.add( 3L );
        queryProbeIds.add( 100L );

        Collection<Long> coexpressedProbeIds = new ArrayList<Long>();
        coexpressedProbeIds.add( this.secondProbeId );
        coexpressedProbeIds.add( 4L );
        coexpressedProbeIds.add( 101L );

        Collection<Long> results = null;

        assertNotNull( ee.getId() );

        results = ppcs.getCoexpressedProbes( queryProbeIds, coexpressedProbeIds, ee, "mouse" );

        assertEquals( 2, results.size() );

        assertFalse( results.contains( 100L ) );
        assertTrue( results.contains( this.firstProbeId ) );
    }

}
