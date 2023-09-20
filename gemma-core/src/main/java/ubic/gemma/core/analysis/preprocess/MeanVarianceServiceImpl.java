/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.analysis.preprocess;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.math.linearmodels.MeanVarianceEstimator;
import ubic.gemma.core.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrixUtil;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedMeanVarianceUpdateEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.MeanVarianceUpdateEvent;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

/**
 * Manage the mean-variance relationship.
 *
 * @author ptan
 */
@Service
public class MeanVarianceServiceImpl implements MeanVarianceService {

    private static final Log log = LogFactory.getLog( MeanVarianceServiceImpl.class );
    private static final ByteArrayConverter bac = new ByteArrayConverter();

    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private ExpressionDataMatrixService expressionDataMatrixService;
    @Autowired
    private AuditTrailService auditTrailService;

    @Override
    @Transactional
    public MeanVarianceRelation create( ExpressionExperiment ee, boolean forceRecompute ) {
        if ( ee == null ) {
            log.warn( "Experiment is null" );
            return null;
        }

        ee = expressionExperimentService.thawLiter( ee );

        MeanVarianceRelation mvr = ee.getMeanVarianceRelation();

        if ( mvr != null && !forceRecompute ) {
            return mvr;
        }

        ExpressionDataDoubleMatrix intensities = expressionDataMatrixService.getProcessedExpressionDataMatrix( ee );
        if ( intensities == null ) {
            throw new IllegalStateException( "Could not locate intensity matrix for " + ee.getShortName() );
        }

        QuantitationType qt = expressionExperimentService.getPreferredQuantitationType( ee );
        if ( qt == null ) {
            throw new IllegalStateException( "Did not find any preferred quantitation type. Mean-variance relation was not computed." );
        }
        try {
            intensities = ExpressionDataDoubleMatrixUtil.filterAndLog2Transform( intensities );
        } catch ( UnsupportedQuantitationScaleConversionException e ) {
            log.warn( "Problem log transforming data. Check that the appropriate log scale is used. Mean-variance will be computed as is." );
        }

        mvr = expressionExperimentService.updateMeanVarianceRelation( ee, calculateMeanVariance( intensities ) );

        auditTrailService.addUpdateEvent( ee, MeanVarianceUpdateEvent.class, "Mean-variance has been updated." );

        log.info( "Mean-variance computation is complete" );

        return mvr;
    }

    /**
     * @param  matrix on which mean variance relation is computed with
     * @return MeanVarianceRelation object
     */
    private MeanVarianceRelation calculateMeanVariance( ExpressionDataDoubleMatrix matrix ) {
        DoubleMatrix2D mat = new DenseDoubleMatrix2D( matrix.rows(), matrix.columns() );
        for ( int row = 0; row < mat.rows(); row++ ) {
            mat.viewRow( row ).assign( matrix.getRawRow( row ) );
        }
        MeanVarianceEstimator mve = new MeanVarianceEstimator( mat );

        MeanVarianceRelation mvr = MeanVarianceRelation.Factory.newInstance();
        if ( mve.getMeanVariance() != null ) {
            mvr.setMeans( bac.doubleArrayToBytes( mve.getMeanVariance().viewColumn( 0 ).toArray() ) );
            mvr.setVariances( bac.doubleArrayToBytes( mve.getMeanVariance().viewColumn( 1 ).toArray() ) );
        }

        return mvr;
    }
}
