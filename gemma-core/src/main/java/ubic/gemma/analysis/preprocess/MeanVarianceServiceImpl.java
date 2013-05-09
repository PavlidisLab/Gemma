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
package ubic.gemma.analysis.preprocess;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.math.MeanVarianceEstimator;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

/**
 * Manage the mean-variance relationship.
 * 
 * @author ptan
 * @version $Id$
 */
@Service
public class MeanVarianceServiceImpl implements MeanVarianceService {

    private static ByteArrayConverter bac = new ByteArrayConverter();
    private static Log log = LogFactory.getLog( MeanVarianceServiceImpl.class );

    /**
     * @param matrix on which mean variance relation is computed with
     * @return MeanVarianceRelation object
     */
    public static MeanVarianceRelation getMeanVariance( ExpressionDataDoubleMatrix matrix ) {

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
        if ( mve.getLoess() != null ) {
            mvr.setLowessX( bac.doubleArrayToBytes( mve.getLoess().viewColumn( 0 ).toArray() ) );
            mvr.setLowessY( bac.doubleArrayToBytes( mve.getLoess().viewColumn( 1 ).toArray() ) );
        }

        return mvr;
    }

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ProcessedExpressionDataVectorCreateHelperService helperService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.preprocess.MeanVarianceService#create(Long, boolean)
     */
    @Override
    public MeanVarianceRelation create( Long eeId, boolean forceRecompute ) {

        log.info( "Starting Mean-variance computation" );

        if ( eeId == null ) {
            log.warn( "No id!" );
            return null;
        }

        ExpressionExperiment ee = expressionExperimentService.load( eeId );
        if ( ee == null ) {
            log.warn( "Could not load experiment with id " + eeId );
            return null;
        }

        MeanVarianceRelation mvr = ee.getMeanVarianceRelation();

        if ( forceRecompute || mvr == null || mvr.getMeans().length == 0 ) {

            Collection<QuantitationType> quantitationTypes = expressionExperimentService.getQuantitationTypes( ee );
            Collection<QuantitationType> usefulQuantitationTypes = ExpressionDataMatrixBuilder
                    .getUsefulQuantitationTypes( quantitationTypes );

            if ( usefulQuantitationTypes.isEmpty() ) {
                throw new IllegalStateException( "No useful quantitation types for " + ee.getShortName() );
            }

            Collection<ProcessedExpressionDataVector> processedVectors = expressionExperimentService
                    .getProcessedDataVectors( ee );
            ExpressionDataDoubleMatrix intensities = helperService.computeIntensities( ee, processedVectors );

            mvr = getMeanVariance( intensities );
            ee.setMeanVarianceRelation( mvr );
            expressionExperimentService.update( ee );
        }

        log.info( "Mean-variance computation is complete" );

        return mvr;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.preprocess.MeanVarianceService#findOrCreate(Long)
     */
    @Override
    public MeanVarianceRelation findOrCreate( Long eeId ) {
        return create( eeId, false );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.preprocess.MeanVarianceService#hasMeanVariance(Long)
     */
    @Override
    public boolean hasMeanVariance( Long eeId ) {
        if ( eeId == null ) {
            log.warn( "No id!" );
            return false;
        }

        ExpressionExperiment ee = expressionExperimentService.load( eeId );
        if ( ee == null ) {
            log.warn( "Could not load experiment with id " + eeId );
            return false;
        }

        return ee.getMeanVarianceRelation() != null;
    }
}
