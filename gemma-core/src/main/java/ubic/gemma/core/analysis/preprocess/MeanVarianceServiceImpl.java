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
import ubic.basecode.math.linearmodels.MeanVarianceEstimator;
import ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionException;
import ubic.gemma.core.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.auditAndSecurity.eventType.MeanVarianceUpdateEvent;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import static ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionUtils.filterAndLog2Transform;

/**
 * Manage the mean-variance relationship.
 *
 * @author ptan
 */
@Service
public class MeanVarianceServiceImpl implements MeanVarianceService {

    private static final Log log = LogFactory.getLog( MeanVarianceServiceImpl.class );

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

        ExpressionDataDoubleMatrix intensities = expressionDataMatrixService.getProcessedExpressionDataMatrix( ee, true );
        if ( intensities == null || intensities.rows() == 0 ) {
            throw new IllegalStateException( "Could not locate intensity matrix, or it was empty, for " + ee.getShortName() );
        }

        if ( !expressionExperimentService.getPreferredQuantitationType( ee ).isPresent() ) {
            throw new IllegalStateException( "Did not find any preferred quantitation type. Mean-variance relation was not computed." );
        }
        try {
            intensities = filterAndLog2Transform( intensities );
        } catch ( QuantitationTypeConversionException e ) {
            log.warn( "Problem log transforming data. Check that the appropriate log scale is used. Mean-variance will be computed as is." );
        }

        if ( intensities.rows() < 3 ) {
            // this data is pretty much useless altogether, so an exception is the right way to go.
            throw new IllegalStateException( "Not enough data left after filtering to proceed (" + intensities.rows() + " rows for " + ee.getShortName() + ")" );
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
            mat.viewRow( row ).assign( matrix.getRowAsDoubles( row ) );
        }
        MeanVarianceEstimator mve = new MeanVarianceEstimator( mat );
        return MeanVarianceRelation.Factory.newInstance(
                mve.getMeanVariance().viewColumn( 0 ).toArray(),
                mve.getMeanVariance().viewColumn( 1 ).toArray() );
    }
}
