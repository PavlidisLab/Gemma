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

import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class Probe2ProbeCoexpressionDaoImplTest extends BaseSpringContextTest {

    ExpressionExperiment ee;
    ExpressionExperimentService ees;
    Long firstProbeId;
    Long secondProbeId;

    Probe2ProbeCoexpressionService ppcs;

    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        this.endTransaction();
        ee = this.getTestPersistentCompleteExpressionExperiment( false );

        ees = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );

        Collection<QuantitationType> qts = ees.getQuantitationTypes( ee );

        // this is bogus, it should represent "pearson correlation" for example, but doesn't matter for this test.
        QuantitationType qt = qts.iterator().next();

        ppcs = ( Probe2ProbeCoexpressionService ) this.getBean( "probe2ProbeCoexpressionService" );

        ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService = ( ProcessedExpressionDataVectorCreateService ) this
                .getBean( "processedExpressionDataVectorCreateService" );

        Collection<ProcessedExpressionDataVector> dvs = processedExpressionDataVectorCreateService
                .computeProcessedExpressionData( ee );

        List<ProcessedExpressionDataVector> dvl = new ArrayList<ProcessedExpressionDataVector>( dvs );

        int j = dvs.size();

        assert j > 0;

        ProbeCoexpressionAnalysis analysis = ProbeCoexpressionAnalysis.Factory.newInstance();
        analysis.setName( "foo" );
        Taxon mouse = this.getTaxon( "mouse" );
        ExpressionExperimentSet se = ExpressionExperimentSet.Factory.newInstance();
        se.getExperiments().add( ee );
        se.setTaxon( mouse );
        se.setName( "bar" );
        analysis.setExpressionExperimentSetAnalyzed( se );

        analysis = ( ProbeCoexpressionAnalysis ) this.persisterHelper.persist( analysis );

        this.firstProbeId = dvl.get( 0 ).getDesignElement().getId();
        this.secondProbeId = dvl.get( 1 ).getDesignElement().getId();

        for ( int i = 0; i < j - 1; i += 2 ) {
            Probe2ProbeCoexpression ppc = MouseProbeCoExpression.Factory.newInstance();
            ppc.setFirstVector( dvl.get( i ) );
            ppc.setSecondVector( dvl.get( i + 1 ) );
            ppc.setMetric( qt );
            ppc.setScore( 0.0 );
            ppc.setPvalue( 0.2 );
            ppc.setExpressionBioAssaySet( ee );
            ppc.setSourceAnalysis( analysis );
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
        /*
         * This would be 6 but we divide the count by 2 as it is assumed we save each link twice.
         */
        assertEquals( 3, countLinks.intValue() );
    }

    /**
     */
    public void testHandleDeleteLinksExpressionExperiment() throws Exception {
        ppcs.deleteLinks( ee );
        Integer countLinks = ppcs.countLinks( ee );
        assertEquals( 0, countLinks.intValue() );
    }

    /**
     * @throws Exception
     */
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
        results = ppcs.validateProbesInCoexpression( queryProbeIds, coexpressedProbeIds, ee, "mouse" );
        assertFalse( results.contains( 100L ) );
        assertTrue( results.contains( this.firstProbeId ) );
    }

}
